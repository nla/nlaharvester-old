package harvester.processor.util;

import harvester.data.HarvestLog;
import harvester.data.Step;
import harvester.processor.data.dao.DAOFactory;

import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.hibernate.Hibernate;

/** provide a wrapper for log4j that can also log to the database's harvest log table.
 *  each instance of this class will cap printing of failed records to user log after 100
 *  of them are printed using that instance.
 *  */
public class StepLoggerImpl implements StepLogger {
	
	private static Logger logger = Logger.getLogger(StepLoggerImpl.class);
	
	private int MAX_FAILED_RECORDS = 10000;
	
	private int harvestid;
	private int failedRecordsLoggedThisHarvest = 0;
	
	private String clienturl;
	
	public StepLoggerImpl(int harvestid, String clienturl) {
		this.harvestid = harvestid;
		this.clienturl = clienturl;
	}
	
	
	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#getOAIIdentifier(org.dom4j.Document)
	 * This is also used in MockStepLogger
	 */
	public String getOAIIdentifier(Document data) {
		List<Node> cnodes = data.selectNodes("comment()");
		
		for(Node n : cnodes) {
			if(n.getText().startsWith("identifier"))
				return n.getText().substring("identifier=".length());			
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#logfailedrecord(int, java.lang.String, org.dom4j.Document, int, java.lang.String, int)
	 */
	public void logfailedrecord(int errorlevel, String description,
			Document data, int position, String name, Integer stepid, int recordnumber) {		
		//extract the oai identifier and embed a link with it
		String msg = description;
		
		try {			
			String identifier = getOAIIdentifier(data);
			
			msg = "Record " + (recordnumber+1) + " rejected<br/>" + 
				  "Processing Step: " + position + " . " + name + "<br/>" + 
				  "Reason: " + msg;
			if(identifier != null)
				  msg = msg + "<br/>OAI ID: <a href='" + clienturl 
				  + "Interact.htm?action=getrecord&amp;harvestid=" + harvestid + "&amp;oaiid=" 
				  + URLEncoder.encode(identifier, "UTF-8") + "' >" + identifier + "</a>";
				
			if(failedRecordsLoggedThisHarvest < MAX_FAILED_RECORDS)
				log(errorlevel, msg, description, stepid, data.asXML());
			else
				locallog(msg, name);
					
		} catch (Exception e) {
			//this is much bad
			logger.info("really bad error occured while logging failed record msg=" + msg);
			
			logger.info("error message: " + e.toString());
			logger.info("stack trace:");
			 
			 for(StackTraceElement el : e.getStackTrace())
				 logger.error(el.toString());
		} finally {
			
			logTooManyMessageIfAtLimit();
			
			failedRecordsLoggedThisHarvest++;
		}
	}

	private void logTooManyMessageIfAtLimit() {
		if(failedRecordsLoggedThisHarvest == MAX_FAILED_RECORDS)
			log(StepLogger.STEP_ERROR, "Logged " + MAX_FAILED_RECORDS + " failed records, no longer recording further failed records", "Hit failed record cap", null, null);
	}
	
	public void logfailedrecord(String description, String reason, Integer stepid, String data) {
		
		if(failedRecordsLoggedThisHarvest < MAX_FAILED_RECORDS)
			log(StepLogger.RECORD_ERROR, description, reason, stepid, data);
		else
			logger.info("hid=" + harvestid + " - " + description);
		
		logTooManyMessageIfAtLimit();
		
		failedRecordsLoggedThisHarvest++;		
	}
	
	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#log(int, java.lang.String, java.lang.String)
	 */
	public void log(int errorlevel, String description, String reason, Integer stepid, String data) {
		Date now = new Date();
		
		HarvestLog hl = new HarvestLog();
		
		//we don't want to overflow the database columns size of 2000(used to be 255) characters
		if(description.length() > 2000)
			hl.setDescription(description.substring(0, 2000));
		else
			hl.setDescription(description);
		
		if(reason != null && reason.length() > 250)
			hl.setReason(reason.substring(0, 250));
		else
			hl.setReason(reason);
		
		hl.setErrorlevel(errorlevel);
		hl.setTimestamp(now);
		
		if(stepid != null) {
			Step step = new Step();
			step.setStepid(stepid);		
			hl.setStep(step);
		}
		
		if(data != null) {					
			try {
			hl.setRecorddata(Hibernate.createBlob(data.getBytes("UTF-8")));
			} catch(Exception e) {
				logger.error("Could save blob");				
			}
			
			hl.setHasdata(1);
		} else hl.setHasdata(0);
		
		hl.setHarvestid(harvestid);
		
		DAOFactory.getDAOFactory().getHarvestlogDAO().AddToDatabase(hl);
		
		logger.info("hid=" + harvestid + " " + errorlevel + " | " + description);
	}

	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#logprop(java.lang.String, java.lang.String)
	 */
	public void logprop(String name, String value, Integer stepid) {
		log(PROP_INFO, name + "=" + value, null, stepid, null);
	}
	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#locallog(java.lang.String, java.lang.String)
	 */
	public void locallog(String msg, String classname) {
		logger.info("hid=" + harvestid + " " + classname + " - " + msg);
	}
	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#log(java.lang.String)
	 */
	public void log(String description) {
		log(INFO, description, null, null, null);
	}
	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#info(java.lang.String)
	 */
	public void info(String description) {
		log(description);
	}
	
	/* (non-Javadoc)
	 * @see harvester.processor.util.StepLogger#error(java.lang.String, java.lang.Throwable)
	 */
	public void error(String description, Throwable excp) {
		logger.error(description, excp);
	}


	public void logreport(String name, String value, Integer stepid) {
		log(REPORT_INFO, name + "=" + value, null, stepid, null);
	}
	
}
