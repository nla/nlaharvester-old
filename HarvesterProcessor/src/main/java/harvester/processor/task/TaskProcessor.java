package harvester.processor.task;

import harvester.data.*;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.data.dao.interfaces.HarvestDAO;
import harvester.processor.email.*;
import harvester.processor.exceptions.*;
import harvester.processor.main.*;
import harvester.processor.steps.StagePluginInterface;
import harvester.processor.util.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.* ;

import org.hibernate.Session;

import java.util.Date;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;

/**
 * The main class that controllers the flow of data through the pipeline and the creation and manipulation
 * of the harvest entry in the database.
 */
public class TaskProcessor implements Runnable, Controller {

	private static Logger logger = Logger.getLogger(TaskProcessor.class);

	public static final Integer KEEP_EXECUTING = 0;
	public static final Integer STOP_EXECUTION = 1;
	public static final Integer SERVER_SHUTTING_DOWN = 2;

	///////////////
	private int profileid;
	private int contributorid;
	private Profile dp = null;
	private Contributor c;
	private int type;
	private String from;
	private String until;
	private int retry;
	private int harvestid;
	///////////////
	private DAOFactory daofactory;
	private HarvestDAO harvestdao;   	
	private Hashtable<String, Integer> stopFlags;
	private Set<Integer> runningContributors;
	private Properties props;
	private ServletContext ctx;
	private SchedulerClient sc;
	private DateFormat userdateformater;
	private String clienturl;
	private TaskUtilities taskUtil;
	private HashMap<String, Object> params;

	@SuppressWarnings("unchecked")
	public TaskProcessor(HashMap<String, Object> params) {
		this.params = params;

		userdateformater = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		userdateformater.setTimeZone(TimeZone.getDefault());

		//do all the DAO factory stuff
		daofactory = DAOFactory.getDAOFactory();
		harvestdao = daofactory.getHarvestDAO();

		int dpid;
		Object retry = params.get("retry");

		if (params.get("profileid") == null || params.get("profileid").equals("")) {
			logger.info("setting profile to default value");
			dpid = -1;
		} else {
			dpid = Integer.parseInt((String) params.get("profileid"));
		}

		if(retry == null)
			retry = "0";

		runningContributors = (Set<Integer>)params.get("runningContributors");

		this.profileid = dpid;
		this.contributorid = Integer.parseInt((String) params.get("contributorid"));
		this.stopFlags = (Hashtable<String, Integer>) params.get("stopFlags");	
		this.type = Integer.parseInt((String) params.get("type"));
		this.retry = Integer.parseInt((String) retry);
		this.from = (String) params.get("from");
		this.until = (String) params.get("until");
		this.props = (Properties) params.get("props");
		this.ctx = (ServletContext) params.get("ctx");
		String task = (String) params.get("task");

		clienturl = (String)props.get("log.clienturl");

		sc = new SchedulerClient(props, this.retry, task);
	}

	/** creates, then starts the pipeline */
	public void run() {

		//Create a new row in the harvester table in the database
		Harvest h = new Harvest();
		if(profileid != -1)
			h.setProfileid(profileid);
		h.setStatus("Running");
		h.setStatuscode(Harvest.RUNNING);
		h.setStarttime(new Date());
		h.setType(type);
		try {
			harvestdao.AddToDatabase(h);
			harvestid = h.getHarvestid();
		} catch(Exception e) {
			logger.error("Unable to create harvest row in table. Erromsg:" + e.toString());
			return;
		}

		logger.info("  harvestid = " + h.getHarvestid());

		//let the stopflags thingy know that we are running
		stopFlags.put(String.valueOf(h.getHarvestid()), TaskProcessor.KEEP_EXECUTING);
		logger.info("added to stopFlags key=" + h.getHarvestid() + " value=" + TaskProcessor.KEEP_EXECUTING);

		// Get the profile and use it to get the list of steps to execute
		LinkedList<harvester.processor.steps.StagePluginInterface> steps = null;

		boolean init_error = false;	//set to true if a failure to initialize occurs
		StepLogger slog = new StepLoggerImpl(harvestid, clienturl);

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().getCurrentSession();
			session.beginTransaction();
			session.update(h);

			if(profileid != -1)
				dp = daofactory.getprofileDAO().getProfile(profileid);
			c = daofactory.getcontributorDAO().getContributor(contributorid);

			h.setContributor(c);

			//initialize things so that I can use them for emails later
			c.getContacts().size();
			c.getContactselections().size();

			taskUtil = new TaskUtilities(harvestid, props, ctx, c);
			taskUtil.logHarvestStatistics(h, c, profileid, retry);

			//set this harvest as the contributors last harvest
			c.setLastharvest(h);
			c.setHidefromworktray(0);	//reset the hide tag

			///////////////////////////////////////////////////////////
			// this from field stuff is messy, mostly because we only want to follow what we get in the passed form field
			// for the first harvest of a string of production harvests. This could be done in a better way if the scheduler
			// had a way to pass different parameters for the first harvest out of a series of scheduled harvests.
			logger.info("passed in from: " + from);

			if(type == Profile.TEST_PROFILE) {
				h.setHarvestfrom(taskUtil.determineCorrectFrom(from, c.getLastsuccessfultest()));		 

			} else { //this is a production harvest then
				// if this a first harvest, we follow the from they pass in, otherwise we do with from last
				if(c.getIsfinishedfirstharvest() != null && c.getIsfinishedfirstharvest() == Contributor.NOT_FIRST_HARVEST ) {
					if(c.getLastsuccessfulprod() != null) {
						logger.info("hid=" + h.getHarvestid() + " " + "harvesting from last successful production harvest minus 1 gran unit");
						//delete = null;

						Calendar cal = Calendar.getInstance();
						cal.setTime(c.getLastsuccessfulprod().getStarttime());
						cal.add(Calendar.DATE, -1);
						h.setHarvestfrom(taskUtil.DatetoOAIFormatUTC(cal.getTime(), c.getGranularity()));	
					} else {
						logger.info("hid=" + h.getHarvestid() + " " + "can't do harvest from last because getLastsuccessfulprod is null");
						h.setHarvestfrom(null);
					}

				} else { //FIRST HARVEST
					h.setHarvestfrom(taskUtil.determineCorrectFrom(from, c.getLastsuccessfulprod()));
				}
			}


			if(until == null || until.equals("") || until.equals("null")) {
				logger.info("until is " + until);
				h.setHarvestuntil(null);
			}
			else
				if(c.getGranularity() == 0) {
					until = until.substring(0, "yyyy-MM-dd".length());
					logger.info("hid=" + h.getHarvestid() + " " + "setting until to shorted date: " + until);
					h.setHarvestuntil(until);
				}
				else {
					logger.info("hid=" + h.getHarvestid() + " " + "setting until to: " + until);
					h.setHarvestuntil(until);
				}

			logger.info("hid=" + h.getHarvestid() + " " + "harvesting from: " + h.getHarvestfrom() + " until: " + h.getHarvestuntil());

			session.update(c);	//harvest from field not needed in database till later, so only update contributor

			if(dp != null)
				logger.debug("hid=" + h.getHarvestid() + " " + "Got profile. number of pipelinestages is:" + dp.getProfilesteps().size());

			//Now that we have the profile, get the step objects using reflection.
			Profiler profiler = new Profiler(params, props, h, ctx, this);
			steps = profiler.getStepObjects(dp);

			//no more need to fiddle with the profile object's collections
			//so close the db connection and reopen when harvest needs to be changed
			session.getTransaction().commit();
			session = null;

		} catch (Exception e) {
			if(session != null)
				session.getTransaction().rollback();

			init_error = true;
			slog.log(StepLogger.INIT_ERROR, "Could not start harvest. Java Error:" + e.toString(), "Could not start harvest", null, null);
			logger.error("error in init", e);	

		}

		if(runningContributors.contains(new Integer(c.getContributorid()))) {
			//this contributor already has a harvest!!!
			//fail this one
			slog.log(StepLogger.INIT_ERROR, "Could not start harvest. There is already a harvest running for this contributor", 
					"Could not start harvest", null, null);
			try {
				sc.reschedule(h);

				h.setEndtime(new Date());
				h.setStatus("FAILED");				
				h.setStatuscode(Harvest.FAILED);
				harvestdao.ApplyChanges(h);
				h.setTotalrecords(0);
				h.setRecordscompleted(0);

				taskUtil.email(Email.HARVEST_FAILURE);

			} catch(Exception e2) {
				logger.error("hid=" + h.getHarvestid() + " " + "database error when applying initialise error msg");
			}

			stopFlags.remove(String.valueOf(h.getHarvestid()));
			return;
		}

		if(init_error) {			 			 
			h.setStatus("FAILED");
			h.setTotalrecords(0);
			h.setRecordscompleted(0);
			h.setStatuscode(Harvest.FAILED);
			try {
				harvestdao.ApplyChanges(h);
			} catch(Exception e2) {
				logger.error("hid=" + h.getHarvestid() + " " + "database error when applying initialise error msg");
			}
			stopFlags.remove(String.valueOf(h.getHarvestid()));
			return;
		}

		//////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////// start the harvest
		try {
			try {
				runningContributors.add(new Integer(c.getContributorid()));
				startPipeline(steps);
				
			} catch (InterruptedException e) {
				logger.info("Interrupted, probably shutting down or stopping", e);
				StagePluginInterface harveststage = steps.getFirst();
				taskUtil.stopHarvest(slog, steps, harveststage, stopFlags); 
			} 
		} catch (Exception e) {
			try{
				logger.error("error caught at task procesor ", e);

				if(e instanceof CustomException) {
					slog.log(StepLogger.STEP_ERROR, ((CustomException)e).getStatusMessage(), ((CustomException)e).getStatusMessage(), null, null);
					h.setStatus( ((CustomException)e).getStatusMessage() );
				} else {
					String msg = (e.getMessage() == null || e.getMessage() == "") ? "" : (", Java Error: " + e.getMessage());
					slog.log(StepLogger.STEP_ERROR, "FAILED" + msg, "Harvest Failure" , null , null);
					h.setStatus("FAILED");
				}

				h.setStatuscode(Harvest.FAILED);
				h.setEndtime(new Date());
				harvestdao.ApplyChanges(h);

				taskUtil.email(Email.HARVEST_FAILURE);
				logger.info("handled exception");
			} catch(Exception e2) {
				try {
					logger.error("database error when applying pipeline error msg", e2);				 	
				}catch (Exception e3){}
			}

		}

		stopFlags.remove(String.valueOf(h.getHarvestid()));
		runningContributors.remove(new Integer(c.getContributorid()));

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/** 
	 * The main pipeline loop is in here.
	 * The harvest step absolutely must be the first in the pipeline, and there must be only one of them! */	
	private void startPipeline(LinkedList<StagePluginInterface> steps) throws Exception {

		StepLogger slog = new StepLoggerImpl(harvestid, clienturl);

		//Stages are already initialised
		//create a new list of records
		Records records = new Records();

		//update local and database logs
		slog.info("Beginning Harvest [Local Time: " + userdateformater.format(new Date()) + "]");
		logger.info("hid=" + harvestid + " " + "stages =" + steps.size());

		StagePluginInterface harveststage = steps.getFirst();
		steps.removeFirst();

		boolean shown_record_count = false;


		//the harvest step will change the continue flag if there is no more work to be done
		while( records.isContinue_harvesting() ) {
			System.gc();

			HibernateUtil.logHibernateStats();

			Integer stop = stopFlags.get(String.valueOf(harvestid));
			Integer shutdown = stopFlags.get("ALL");
			yield();

			//load harvest object again
			Harvest h = harvestdao.getHarvest(harvestid);

			logger.info("Running stage: " + harveststage.getName());

			try {
				records = harveststage.Process(records);
			} catch (UnableToConnectException ue) {
				logger.info("hid=" + h.getHarvestid() + " " + "add a retry event to the scheduler");
				h.setEndtime(new Date());
				h.setStatus(sc.reschedule(h));
				h.setStatuscode(Harvest.FAILED);
				harvestdao.ApplyChanges(h);

				if(sc.getLastScheduleCode() == SchedulerClient.FAILED)
					taskUtil.email(Email.HARVEST_FAILURE);
				else
					taskUtil.email(Email.HARVEST_ERRORS);

				return;

			} catch (Exception e) {
				logger.error("Error harvesting with: " + harveststage.getName(), e);
				//otherwise the gui reports failed records when none have failed!
				h.setTotalrecords(h.getTotalrecords() + records.getTotalRecords());
				h.setRecordscompleted(h.getRecordscompleted() + records.getCurrentrecords());

				daofactory.getHarvestdataDAO().AddToDatabaseBulk(records.getRecords(), h.getHarvestid(), harveststage.getPosition());

				throw new HarvestException();
			}

			if(shown_record_count == false && records.getRecordsinsource() != null) {
				slog.log("Found " + records.getRecordsinsource() + " records in source.");
				shown_record_count = true;
			}

			slog.log("Harvested " + (h.getTotalrecords() + records.getTotalRecords()) + " records so far... [Local Time: " + userdateformater.format(new Date()) + "]");

			try {
				for( StagePluginInterface spi : steps) {
					try {
						records = spi.Process(records);
					} catch (Exception e) {
						logger.error("Error processing stage: " + spi.getName());

						h.setTotalrecords(h.getTotalrecords() + records.getTotalRecords());
						h.setRecordscompleted(h.getRecordscompleted() + records.getCurrentrecords());	//otherwise the gui reports failed records when none have failed!

						logger.info("hid=" + h.getHarvestid() + " " + "updating record counts for gui total=" 
								+ h.getTotalrecords() + " current=" + h.getRecordscompleted());

						//they might want to view the records to see what went wrong
						//set the stage for the data as the stage before the crash
						daofactory.getHarvestdataDAO().AddToDatabaseBulk(records.getRecords(), h.getHarvestid(), spi.getPosition());

						throw e;
					}

				}
			} finally {
				h.setTotalrecords(h.getTotalrecords() + records.getTotalRecords());
				h.setRecordscompleted(h.getRecordscompleted() + records.getCurrentrecords());
				h.setDeletionsread(h.getDeletionsread() + records.getDeletedRecords().size());
				h.setDeletionsperformed(h.getDeletionsperformed() + records.getDeletionsPerformed());
				h.setRecordsadded(h.getRecordsadded() + records.getRecordsAdded());
			}

			//add all the records in the database 'staging' area, so that the view records button works
			daofactory.getHarvestdataDAO().AddToDatabaseBulk(records.getRecords(), h.getHarvestid(), steps.size()+1 );

			harvestdao.ApplyChanges(h);

			records.setTotalRecords(0);
			records.setDeletionsPerformed(0);
			records.setRecordsAdded(0);
			records.setRecords(new LinkedList<Object>());
			records.setDeletedRecords(new LinkedList<Object>());

		}

		////////////////////////////////FINISHED HARVESTING

		//dispose everything
		harveststage.Dispose();

		for(StagePluginInterface spi : steps) {
			spi.Dispose();
		}		

		//load harvest object again
		Harvest h = harvestdao.getHarvest(harvestid);

		//update finish time and records completed
		h.setEndtime(new Date());
		h.setStatus("Complete");
		h.setStatuscode(Harvest.SUCCESSFUL);

		if(h.getTotalrecords() != h.getRecordscompleted())
			taskUtil.email(Email.RECORD_FAILURES);
		else
			taskUtil.email(Email.SUCCESS);

		harvestdao.ApplyChanges(h);
		harvestdao.makeLastSuccHarvest(h.getHarvestid(), c.getContributorid(), type);	

		//update local logs
		logger.info("hid=" + h.getHarvestid() + " " + "finished pipeline with " +  h.getRecordscompleted() + " records");
		slog.log(" Harvest Complete [Local Time: " + userdateformater.format(new Date()) + "]");
	}

	public void yield() throws InterruptedException {
		Integer stop = stopFlags.get(String.valueOf(harvestid));
		Integer shutdown = stopFlags.get("ALL");

		if(STOP_EXECUTION.equals(stop) || SERVER_SHUTTING_DOWN.equals(shutdown) ) {
			throw new InterruptedException("String stopped at yield point");
		}
	}

	public int getHarvestid() {
		return harvestid;
	}

	public int getContributorid() {
		return contributorid;
	}

}
