package harvester.processor.task;

import harvester.data.Contributor;
import harvester.data.Harvest;
import harvester.data.Profile;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.email.Email;
import harvester.processor.steps.StagePluginInterface;
import harvester.processor.util.HibernateUtil;
import harvester.processor.util.StepLogger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.hibernate.stat.Statistics;

public class TaskUtilities {
	
	private static Logger logger = Logger.getLogger(TaskUtilities.class);
	
   	private Properties props;
	private DAOFactory daofactory;
   	private ServletContext ctx;
   	//private SchedulerClient sc;
   	private int harvestid;
   	private Contributor c;
   	
   	public TaskUtilities(int harvestid, Properties props, ServletContext ctx, Contributor c) {
   		daofactory = DAOFactory.getDAOFactory();
   		this.props = props;
   		this.ctx = ctx;
   		this.c = c;
   		this.harvestid = harvestid;
   	}
   	
	/**
	 * If a user has set them selves up to be notified of harvest results, we email them here
	 * @param success status of just finished harvest
	 */
	public void email(int success) {
		if(props.get("mail.on") != null && props.get("mail.on").equals("true")) {
			try {
				Harvest h = daofactory.getHarvestDAO().getHarvest(harvestid);
				 logger.info("hid=" + harvestid + " " + "setting up email module");
				 harvester.processor.email.Email email = new harvester.processor.email.Email();
				 email.setC(c);
				 email.setH(h);
				 email.setProps(props);
				 email.setCtx(ctx);
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
	 * Set status and other details correctly for a stopped harvest
	 */
	public void stopHarvest(StepLogger slog, LinkedList<StagePluginInterface> steps, 
							StagePluginInterface harveststage, Hashtable<String, Integer> stopFlags) throws Exception {
		
		DateFormat userdateformater = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		userdateformater.setTimeZone(TimeZone.getDefault());
		
		//We need to call dispose on everything
		harveststage.Dispose();
		
		for(StagePluginInterface spi : steps) {
			spi.Dispose();
		}	

		Integer shutdown = stopFlags.get("ALL");
				
		if(TaskProcessor.SERVER_SHUTTING_DOWN.equals(shutdown)) {											
			slog.info("Harvest stopped since server is shutting down. [Local Time: " + userdateformater.format(new Date()) + "]");
		}
		else {
			slog.info("Harvest stopped by user. [Local Time: " + userdateformater.format(new Date()) + "]");
		}
		
		Harvest h = daofactory.getHarvestDAO().getHarvest(harvestid);
		h.setStatuscode(Harvest.SUCCESSFUL);
		h.setStatus("Stopped");
		h.setEndtime(new Date());
		daofactory.getHarvestDAO().ApplyChanges(h);
	}
		
	/**
	 * Coverts a standard java date to the specific oai style date
	 * We always compute both the short and long so that we can log the long version.
	 * @param lastharvest the date to convert
	 * @param granularity A flag indicating the granularity of the date needed. ( 0 is short format)
	 * @return the new date
	 */
	public String DatetoOAIFormatUTC(Date lastharvest, Integer granularity) {
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
				//delete = null;					
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
	
	public void logHarvestStatistics(Harvest h, Contributor c, int profileid, int retry) {
		logger.info("******************************************************");
		logger.info("  Task Processor started");
		logger.info("  Maximum amount of memory the VM will attemp to use = " + Runtime.getRuntime().maxMemory()/1024 + " KiloBytes");
		logger.info("  harvestid = " + harvestid);
		logger.info("  profileid = " + profileid);
		logger.info("  Harvest type = " + (h.getType() == Profile.TEST_PROFILE ? "Test" : "Production" ));
		logger.info("  retry = " + retry);
		logger.info("  Contributor id = " + c.getContributorid() + " Contributor name=" + c.getName());
		logger.info("  Collection id = " + c.getCollection().getCollectionid() + " Collection name=" + c.getCollection().getName());
		if(c.getLastsuccessfulprod() != null)
			logger.info("  Contributor last successful production harvest's date: " + c.getLastsuccessfulprod().getStarttime());
		if(c.getLastsuccessfultest() != null)
			logger.info("  Contributor last successful test harvest's date: " + c.getLastsuccessfultest().getStarttime());		 
		if(c.getLastharvest() != null)
			logger.info("  Contributor last harvest's date: " + c.getLastharvest().getStarttime());
		logger.info("******************************************************");
		 
	}
}
