package harvester.processor.util;

import harvester.data.*;
import harvester.processor.task.TaskProcessor;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

/** handles rescheduling of tasks by calling the scheduler ws */
public class SchedulerClient 
{
	private static Logger logger = Logger.getLogger(SchedulerClient.class);
	private static Format cronformatter = new SimpleDateFormat("15 mm HH dd MM ? yyyy");
	private static Format statusMessageDateFormat = new SimpleDateFormat("EEE dd MMM HH:mm"); 
	
	private static String FAILED_TO_CONNECT = "Failed to connect";
	
	public static int RESCEHDULED = 0;
	public static int FAILED = 1;
	
	private int scheduleCode;
	private int retry;
	private String task;
	private String clientUrl;
	
	private Properties props;
	
	public int getLastScheduleCode() {
		return scheduleCode;
	}
	
	public SchedulerClient(Properties props, int retry, String task) {
		this.retry = retry;
		this.task = task;
		this.props = props;
		clientUrl = (String)props.get("log.clienturl");
	}
	
	/**
	 * reschedule the passed task
	 * @param tp the task procesor encapsulating the current state
	 * @return schedule code
	 * @throws Exception
	 */
	public String reschedule(Harvest h) throws Exception {
		StepLogger slog = new StepLoggerImpl(h.getHarvestid(), clientUrl);
		
		String scheduleretry = props.getProperty("scheduleretry");
		String thisurl = props.getProperty("thisurl");
		
		if(h.getType() == Profile.TEST_PROFILE && !"true".equals(props.getProperty("retryfortest"))) {
			//do not retry on a test harvest, unless set in the config file
			logger.info("not scheduling a retrying for this test harvest");
			scheduleCode = FAILED;
			return FAILED_TO_CONNECT;
		}
		
		if( "false".equals(scheduleretry) ) {
			logger.info("not attempting to reschedule, since it has been turned off in the configuration files");
			scheduleCode = FAILED;
			return FAILED_TO_CONNECT;	//no need to log that we a re doing this
		}
		
		if(retry >= 2) {
			logger.info("used up all retries, just fail");
			scheduleCode = FAILED;
			slog.log(StepLogger.STEP_ERROR, "Unable to connect, all retries failed", "Unable to connect", null, null);
			return "FAILURE: Unable to connect";
		}
		//get the two urls we need
		String schedulerurl = props.getProperty("schedulerurl");
		String scheduletesting = props.getProperty("scheduletesting");
		String msg = FAILED_TO_CONNECT;
		Calendar cal = Calendar.getInstance();

		if(scheduletesting != null && scheduletesting.equals("true")) {
			logger.info("using testing time increments 1,1min");
			if(retry == 0)
				cal.add(Calendar.MINUTE, 1);
			else if(retry == 1)
				cal.add(Calendar.MINUTE, 1);
			else cal =null;
		} else {
			if(retry == 0)
				cal.add(Calendar.HOUR, 1);
			else if(retry == 1)
				cal.add(Calendar.HOUR, 12);
			else cal=null;
		}
		
		String cron = null;
		if(cal != null)	
		{
			Date timetodo =  cal.getTime();
			slog.log(StepLogger.STEP_ERROR,"Rescheduled to " + statusMessageDateFormat.format(timetodo), "Rescheduled", null, null);
			msg = "Rescheduled to " + statusMessageDateFormat.format(timetodo); //"WARNING: Unable to connect"; //, retrying at: " + timetodo.toString();
			cron = cronformatter.format(timetodo);
		}
		
		//increment the retry
		retry++;
		
		logger.info("scheduling a retry schedule, with retrynum= " + retry);
		
		StringBuilder doc = new StringBuilder();
		doc.append("retry=" + retry);
		if(cron != null)
		{
			doc.append("&cron=");
			doc.append(URLEncoder.encode(cron, "UTF-8"));
		}
		
		doc.append("&contributorid=" + h.getContributor().getContributorid());
		if(h.getProfileid() != -1)
			doc.append("&profileid=" + h.getProfileid());
		doc.append("&task=" + task);
		doc.append("&type=" + h.getType());
		if(h.getHarvestfrom() != null && !h.getHarvestfrom().equals(""))
			doc.append("&from=" + h.getHarvestfrom());
		if(h.getHarvestuntil() != null && !h.getHarvestuntil().equals(""))
		doc.append("&until=" + h.getHarvestuntil());
		doc.append("&url=" + URLEncoder.encode(thisurl, "UTF-8"));
		doc.append("&action=start");
	
		
		String url = schedulerurl + h.getContributor().getContributorid();
		logger.info("url : " + url  + " \ndoc : " + doc.toString());
				
		logger.info("connecting to scheduler ws");
		URL requesturl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
		
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
	    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.connect();
		
	    Writer w = new OutputStreamWriter(conn.getOutputStream());
		w.write(doc.toString());
		w.flush();
		
		logger.info("Response code:" + conn.getResponseCode());
		
		conn.disconnect();
		logger.info("disconnected successfully");

		
		scheduleCode = RESCEHDULED;
		
		return msg;
	}
}
