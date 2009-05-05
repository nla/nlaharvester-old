package harvester.processor.task;

import harvester.data.*;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.data.dao.interfaces.HarvestDAO;
import harvester.processor.email.*;
import harvester.processor.exceptions.*;
import harvester.processor.main.Records;
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
public class TaskProcessor implements Runnable {

	private static Logger logger = Logger.getLogger(TaskProcessor.class);
	
	public static final Integer KEEP_EXECUTING = 0;
	public static final Integer STOP_EXECUTION = 1;
	public static final Integer SERVER_SHUTTING_DOWN = 2;
	
	///////////////
	private int profileid;
	private int contributorid;
	private Profile dp;
	private Contributor c;
	private Harvest h;
	private int  task;
	private int type;
	private String from;
	private String until;
   	private int retry;
   	private String delete;
   	private String until50;
   	private String singlerecord;	
	///////////////
	private DAOFactory daofactory;
	private HarvestDAO harvestdao;   	
	private Hashtable<String, Integer> stopFlags;
	private Set<Integer> runningContributors;
   	private Properties props;
   	private ServletContext ServletCtx;
   	private SchedulerClient sc;
	private DateFormat userdateformater;
	private String clienturl;

	@SuppressWarnings("unchecked")
	public TaskProcessor(HashMap<String, Object> params) {
		
		userdateformater = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		userdateformater.setTimeZone(TimeZone.getDefault());
		
		sc = new SchedulerClient();
		
		dp = null;
		//do all the DAO factory stuff
		daofactory = DAOFactory.getDAOFactory();
		harvestdao = daofactory.getHarvestDAO();
		
		int dpid;
		Object retry = params.get("retry");
		
		if (params.get("profileid") == null || params.get("profileid").equals("")) {
			logger.info("setting profile to default value");
			dpid = -1;
		}
		else
			dpid = Integer.parseInt((String) params.get("profileid"));
		
		if(retry == null)
			retry = "0";
		
		runningContributors = (Set<Integer>)params.get("runningContributors");
		
		this.setProfileid(dpid);
		this.setContributorid(Integer.parseInt((String) params.get("contributorid")));
		this.setTask(Integer.parseInt((String) params.get("task")));
		this.setStopFlags((Hashtable<String, Integer>) params.get("stopFlags"));	
		this.setType(Integer.parseInt((String) params.get("type")));
		this.setRetry(Integer.parseInt((String) retry));
		this.setFrom((String) params.get("from"));
		this.setUntil((String) params.get("until"));
		this.setProps((Properties) params.get("props"));
		this.setServletCtx((ServletContext) params.get("ctx"));
		this.setDelete((String)params.get("delete"));
		this.setUntil50((String)params.get("until50"));
		this.setSinglerecord((String)params.get("singlerecord"));
		
		clienturl = (String)props.get("log.clienturl");
	}

	/** creates, then starts the pipeline */
	public void run() {
			
		//Create a new row in the harvester table in the database
		 h = new Harvest();
		 if(profileid != -1)
			 h.setProfileid(profileid);
		 h.setStatus("Running");
		 h.setStatuscode(Harvest.RUNNING);
		 h.setStarttime(new Date());
		 h.setType(type);
		 try {
			 harvestdao.AddToDatabase(h);
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
		 StepLogger slog = new StepLoggerImpl(h.getHarvestid(), clienturl);
		 
		 try {
			 Session session = HibernateUtil.getSessionFactory().getCurrentSession();
			 session.beginTransaction();
			 
			 if(profileid != -1)
				 dp = daofactory.getprofileDAO().getProfile(profileid);
			 c = daofactory.getcontributorDAO().getContributor(contributorid);
			 
			 h.setContributor(c);
			 
			logger.info("******************************************************");
			logger.info("  Task Processor started");
			logger.info("  Maximum amount of memory the VM will attemp to use = " + Runtime.getRuntime().maxMemory()/1024 + " KiloBytes");
			logger.info("  harvestid = " + h.getHarvestid());
			logger.info("  profileid = " + profileid);
			logger.info("  Harvest type = " + (type == Profile.TEST_PROFILE ? "Test" : "Production" ));
			logger.info("  retry = " + retry);
			logger.info("  Contributor id = " + contributorid + " Contributor name=" + c.getName());
			logger.info("  Collection id = " + c.getCollection().getCollectionid() + " Collection name=" + c.getCollection().getName());
			logger.info("******************************************************");
			 
			 //initialize things so that I can use them for emails later
			 c.getContacts().size();
			 c.getContactselections().size();
			 
			 
			 //set this harvest as the contributors last harvest
			 c.setLastharvest(h);
			 c.setHidefromworktray(0);	//reset the hide tag
			 
			 //update the from value in the harvest record and contributor
			 logger.info("passed in from: " + from);
			 if(c.getLastsuccessfulprod() != null)
				 logger.info("hid=" + h.getHarvestid() + " " + "contributor last successful production harvest's date: " + c.getLastsuccessfulprod().getStarttime());
			 if(c.getLastsuccessfultest() != null)
				 logger.info("hid=" + h.getHarvestid() + " " + "contributor last successful test harvest's date: " + c.getLastsuccessfultest().getStarttime());		 
			 if(c.getLastharvest() != null)
				 logger.info("hid=" + h.getHarvestid() + " " + "contributor last harvest's date: " + c.getLastharvest().getStarttime());
			 
			 ///////////////////////////////////////////////////////////
			 // this from field stuff is messy, mostly because we only want to follow what we get in the passed form field
			 // for the first harvest of a string of production harvests. This could be done in a better way if the scheduler
			 // had a way to pass different parameters for the first harvest out of a series of scheduled harvests.
			 
			 
			 if(type == Profile.TEST_PROFILE) {
				 h.setHarvestfrom(determineCorrectFrom(from, c.getLastsuccessfultest()));		 
				 
			 } else { //this is a production harvest then
				 // if this a first harvest, we follow the from they pass in, otherwise we do with from last
				 if(c.getIsfinishedfirstharvest() != null && c.getIsfinishedfirstharvest() == Contributor.NOT_FIRST_HARVEST ) {
					 if(c.getLastsuccessfulprod() != null) {
						logger.info("hid=" + h.getHarvestid() + " " + "harvesting from last successful production harvest minus 1 gran unit");
						delete = null;
						 
						Calendar cal = Calendar.getInstance();
						cal.setTime(c.getLastsuccessfulprod().getStarttime());
						cal.add(Calendar.DATE, -1);
						h.setHarvestfrom(DatetoOAIFormatUTC(cal.getTime(), c.getGranularity()));	
					 } else {
						 logger.info("hid=" + h.getHarvestid() + " " + "can't do harvest from last because getLastsuccessfulprod is null");
						 h.setHarvestfrom(null);
					 }
					 
				 } else { //FIRST HARVEST
					 h.setHarvestfrom(determineCorrectFrom(from, c.getLastsuccessfulprod()));
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
			 steps = Profiler.getStepObjects(this);
			 
			 //no more need to fiddle with the profile object's collections
			 //so close the db connection and reopen when harvest needs to be changed
			 session.getTransaction().commit();
			
			 
		 } catch (Exception e) {			 
			 init_error = true;
			 slog.log(StepLogger.INIT_ERROR, "Could not start harvest. Java Error:" + e.toString(), "Could not start harvest", null, null);
			 for(StackTraceElement el : e.getStackTrace())
				 logger.error(el.toString());				
		 }
		 
		 if(runningContributors.contains(new Integer(c.getContributorid()))) {
			 //this contributor already has a harvest!!!
			 //fail this one
			 slog.log(StepLogger.INIT_ERROR, "Could not start harvest. There is already a harvest running for this contributor", 
					 "Could not start harvest", null, null);
			 try {
				sc.reschedule(this);
				
				h.setEndtime(new Date());
				h.setStatus("FAILED");				
				h.setStatuscode(Harvest.FAILED);
				harvestdao.ApplyChanges(h);
				h.setTotalrecords(0);
				h.setRecordscompleted(0);
				
				email(Email.HARVEST_FAILURE);
				
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
			 runningContributors.add(new Integer(c.getContributorid()));
			 startPipeline(steps);

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
				 
				email(Email.HARVEST_FAILURE);
				logger.info("handled exception");
			 } catch(Exception e2) {
				 try {
				 	logger.error("database error when applying pipeline error msg", e2);				 	
				 }catch (Exception e3){}
			 }

		 }
		 
		 if(HibernateUtil.getSessionFactory().getCurrentSession() != null)
			 HibernateUtil.getSessionFactory().getCurrentSession().close();
		 
		 stopFlags.remove(String.valueOf(h.getHarvestid()));
		 runningContributors.remove(new Integer(c.getContributorid()));
		 
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/** 
	 * The main pipeline loop is in here.
	 * The harvest step absolutely must be the first in the pipeline, and there must be only one of them! */	
	private void startPipeline(LinkedList<StagePluginInterface> steps) throws Exception {
		
		StepLogger slog = new StepLoggerImpl(h.getHarvestid(), clienturl);
		
		//Stages are already initialised
		//create a new list of records
		Records records = new Records();
		
		//update local and database logs
		slog.info("Beginning Harvest [Local Time: " + userdateformater.format(new Date()) + "]");
		logger.info("hid=" + h.getHarvestid() + " " + "stages =" + steps.size());
		
		StagePluginInterface harveststage = steps.getFirst();
		steps.removeFirst();
		
		boolean shown_record_count = false;
		
		harvestdao.ApplyChanges(h);
		
		//the harvest step will change the continue flag if there is no more work to be done
		while( records.isContinue_harvesting() ) {
				logger.debug("##############");
				logger.debug("Maximum amount of memory the VM will attemp to use = " + Runtime.getRuntime().maxMemory()/1024 + " KiloBytes");
				logger.debug("Current amount of free memory availible for future allocation: " + Runtime.getRuntime().freeMemory()/1024 + " Kilobytes");
				logger.debug("Total amount of memory in the JVM: " + Runtime.getRuntime().totalMemory()/1024 + " Kilobytes");
				logger.debug("##############");
			
				Integer stop = stopFlags.get(String.valueOf(h.getHarvestid()));
				Integer shutdown = stopFlags.get("ALL");
				if(STOP_EXECUTION.equals(stop) || SERVER_SHUTTING_DOWN.equals(shutdown) ) {
					if(SERVER_SHUTTING_DOWN.equals(shutdown)) {											
						slog.info("Harvest stopped since server is shutting down. [Local Time: " + userdateformater.format(new Date()) + "]");
					}
					else
						slog.info("Harvest stopped by user. [Local Time: " + userdateformater.format(new Date()) + "]");
					h.setStatuscode(Harvest.SUCCESSFUL);
					h.setStatus("Stopped");
					h.setEndtime(new Date());
					harvestdao.ApplyChanges(h);
					return;
				}
					
			
			logger.info("Running stage: " + harveststage.getName());
			
			try {
				records = harveststage.Process(records);
			} catch (UnableToConnectException ue) {
				logger.info("hid=" + h.getHarvestid() + " " + "add a retry event to the scheduler");
				h.setEndtime(new Date());
				h.setStatus(sc.reschedule(this));
				h.setStatuscode(Harvest.FAILED);
				harvestdao.ApplyChanges(h);
				
				if(sc.getLastScheduleCode() == SchedulerClient.FAILED)
					email(Email.HARVEST_FAILURE);
				else
					email(Email.HARVEST_ERRORS);
				
				return;
				
			} catch (InterruptedException  e) {
				logger.info("Thread was interrupted, probably shutting down", e);
				continue;				
			} catch (Exception e) {
				logger.error("Error harvesting with: " + harveststage.getName(), e);
				//otherwise the gui reports failed records when none have failed!
				h.setTotalrecords(h.getTotalrecords() + records.getTotalRecords());
				h.setRecordscompleted(h.getRecordscompleted() + records.getCurrentrecords());
				
				daofactory.getHarvestdataDAO().AddToDatabaseBulk(records.getRecords(), h.getHarvestid(), harveststage.getPosition());
				
				throw new HarvestException();
			}
			
			if(shown_record_count == false && records.getRecordsinsource() != null)
			{
				slog.log("Found " + records.getRecordsinsource() + " records in source.");
				shown_record_count = true;
			}
			
			slog.log("Harvested " + (h.getTotalrecords() + records.getTotalRecords()) + " records so far... [Local Time: " + userdateformater.format(new Date()) + "]");
						
			try {
				for( StagePluginInterface spi : steps) {
					try {
						records = spi.Process(records);
					} catch (InterruptedException  e) {
						logger.info("Thread was interrupted, probably shutting down", e);
						throw e;
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
			} catch (InterruptedException  e) {
				logger.info("Thread was interrupted, probably shutting down", e);
				break;
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
		
		//update finish time and records completed
		h.setEndtime(new Date());
		//h.setRecordscompleted(current_records);
		//h.setTotalrecords(total_records);
		h.setStatus("Complete");
		h.setStatuscode(Harvest.SUCCESSFUL);
		
		if(h.getTotalrecords() != h.getRecordscompleted())
			email(Email.RECORD_FAILURES);
		else
			email(Email.SUCCESS);
		
		harvestdao.ApplyChanges(h);
		
		harvestdao.makeLastSuccHarvest(h.getHarvestid(), c.getContributorid(), type);	
			
		
		//update local logs
		logger.info("hid=" + h.getHarvestid() + " " + "finished pipeline with " +  h.getRecordscompleted() + " records");
		slog.log(" Harvest Complete [Local Time: " + userdateformater.format(new Date()) + "]");
	}
	
	/**
	 * If a user has set them selves up to be notified of harvest results, we email them here
	 * @param success status of just finished harvest
	 */
	private void email(int success) {
		if(props.get("mail.on") != null && props.get("mail.on").equals("true")) {
			try {
				logger.info("hid=" + h.getHarvestid() + " " + "setting up email module");
				 harvester.processor.email.Email email = new harvester.processor.email.Email();
				 email.setC(c);
				 email.setH(h);
				 email.setProps(props);
				 email.setCtx(ServletCtx);
				 email.EmailInit();
				 if(success == Email.SUCCESS)
					 email.emailSuccess();
				 else if( success == Email.HARVEST_ERRORS)
					 email.emailHarvestErrors();
				 else if( success == Email.RECORD_FAILURES)
					 email.emailRecordFailure();			 
				 else if( success == Email.HARVEST_FAILURE)
					 email.emailHarvestFailure();
			} catch (Exception e) {
				logger.info("unable to email contacts:" + e.toString());
				 for(StackTraceElement el : e.getStackTrace())
					 logger.error(el.toString());
				 
			}
		} else logger.info("emailing turned off");
	}
	
	/**
	 * Coverts a standard java date to the specific oai style date
	 * We always compute both the short and long so that we can log the long version.
	 * @param lastharvest the date to convert
	 * @param granularity A flag indicating the granularity of the date needed. ( 0 is short format)
	 * @return the new date
	 */
	private String DatetoOAIFormatUTC(Date lastharvest, Integer granularity) {
		if (lastharvest == null)
			return null;
		
		logger.info("converting to UTC, old date = " + lastharvest.toString());
		DateFormat shortoai = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat oai = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");		
		oai.setTimeZone( TimeZone.getTimeZone("UTC"));
		shortoai.setTimeZone( TimeZone.getTimeZone("UTC"));
		
		logger.info("utc converted date is: " + oai.format(lastharvest));
		
		String d = granularity == 0 ? shortoai.format(lastharvest) : oai.format(lastharvest);
		logger.info("new date = " + d);
		return d;
	}
	
	/**
	 * takes from field information in the format passed to the servlet and determines an actual date to use for the from field
	 * @param from the from string passed into the servlet
	 * @param succharvest the harvest data object from the last successful harvest of the type needed
	 * @return a oai formatted from date or null.
	 */
	public String determineCorrectFrom(String from, Harvest succharvest) {
		String finalfrom = null;
		
		 if(from == null || from.equals("")) {
			 logger.error("NO FROM FIELD PASSED ERRROR!!!!");
		 } else if(from.equals("LASTRUN")) {
			 logger.info("harvesting from last harvest of this type");
			 if(succharvest != null) {
				logger.info("harvesting from last successful harvest of this type minus 1 day");
				delete = null;					
				Calendar cal = Calendar.getInstance();
				cal.setTime(succharvest.getStarttime());
				cal.add(c.getGranularity() == 0 ? Calendar.DATE : Calendar.MINUTE, -1);
				finalfrom = DatetoOAIFormatUTC(cal.getTime(), c.getGranularity());					 
			 } else {
				 logger.info("no last harvest!!!!, setting to from earliest");
			 }			 
	 	 } else if( from.equals("FIRST")) {
			 logger.info("harvesting from first record");
		 } else {
			 finalfrom = c.getGranularity() == 0 ? from.substring(0, "yyyy-MM-dd".length()) : from;
			logger.info("harvesting from specified from: " + from); 
		 }	
		 
		 return finalfrom;
	}
	
	
	
	
	

	public Profile getDp() {
		return dp;
	}

	public void setDp(Profile dp) {
		this.dp = dp;
	}

	public Contributor getC() {
		return c;
	}

	public void setC(Contributor c) {
		this.c = c;
	}

	public Harvest getH() {
		return h;
	}

	public void setH(Harvest h) {
		this.h = h;
	}

	public int getProfileid() {
		return profileid;
	}

	public void setProfileid(int dataprofileid) {
		this.profileid = dataprofileid;
	}

	public int getContributorid() {
		return contributorid;
	}

	public void setContributorid(int contributorid) {
		this.contributorid = contributorid;
	}

	public int getTask() {
		return task;
	}

	public void setTask(int task) {
		this.task = task;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getUntil() {
		return until;
	}

	public void setUntil(String until) {
		this.until = until;
	}

	public Hashtable<String, Integer> getStopFlags() {
		return stopFlags;
	}

	public void setStopFlags(Hashtable<String, Integer> stopFlags) {
		this.stopFlags = stopFlags;
	}

	public Properties getProps() {
		return props;
	}

	public void setProps(Properties props) {
		this.props = props;
	}
	public ServletContext getServletCtx() {
		return ServletCtx;
	}

	public void setServletCtx(ServletContext servletCtx) {
		ServletCtx = servletCtx;
	}
	

   	public int getRetry() {
		return retry;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}

	public String getUntil50() {
		return until50;
	}

	public void setUntil50(String until50) {
		this.until50 = until50;
	}
	
	public String getSinglerecord() {
		return singlerecord;
	}

	public void setSinglerecord(String singlerecord) {
		this.singlerecord = singlerecord;
	}
	
	public String getClienturl() {
		return clienturl;
	}
}
