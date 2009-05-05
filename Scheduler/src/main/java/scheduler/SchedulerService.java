package scheduler;

import java.text.*;
import java.util.*;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import org.apache.log4j.Logger;
import org.quartz.*;

/**
 *  This class contains all logic related to the translating of requests into commands to 
 *  the quartz scheduler, which is used internally to handle the scheduling.
 *  http://www.opensymphony.com/quartz/
 *  
 * @author adefazio
 *
 */
public class SchedulerService {
	
	private static Logger logger = Logger.getLogger(SchedulerService.class);
	
	public Scheduler s;
	
	public SchedulerService(Scheduler s) {
		this.s = s;
	}

	public String getschedule(List<String> jobids) throws Exception{
		
		StringBuilder sb = new StringBuilder();
		int JobsThatExistCount = 0;
		
		logger.info("jobid list = " + jobids);
		
		try{
			sb.append("<?xml version=\"1.0\"?>");	//attach the header
			sb.append("<schedules>");
			for(String id : jobids)
			{
				JobDetail jd = null;
				try 
				{
					//get the schedule information from the schedule object
					jd = s.getJobDetail(id, Scheduler.DEFAULT_GROUP);
					if(jd == null)
					{
						logger.error("no Job with id " + id + " found");
						continue;
					}
					JobsThatExistCount++;
					logger.info("found jobid = " + id);
				} catch (SchedulerException e) 
				{
					logger.error("Exception while fetching job " + id);
					sb.append("</schedules>");	
					logger.error(sb.toString(), e);
					return sb.toString();
				}	
				//create the xml document
				serialise(jd, sb);
			}
			sb.append("</schedules>");
			
			//we want to tell them if we didn't find anything
			if(JobsThatExistCount == 0)
				throw new Exception("didn't find anything");
			
		} catch (Exception e)
		{			
			logger.error("unable to create the sb xml doc", e);
			throw new Exception("error occured");
		}
		String result = sb.toString();
		logger.debug(result);
		return result;
	}
	
	/**
	 * Converts a job detail object into an xml schedule
	 * @param jd
	 * @param sb the xml is added onto this string builder
	 * @throws Exception
	 */
	private void serialise(JobDetail jd, StringBuilder sb) throws Exception {
		
		//UTC: 2007-07-15 06:01
		DateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		utc.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		Date Previous= null;
		Date Next = null;
		String enabled;
		if(s.getTriggerState("0", jd.getName()) == Trigger.STATE_PAUSED)
			enabled = "false";
		else
			enabled = "true";
		
		
		sb.append("<schedule id=\""
				+ jd.getName() +"\" enabled=\""
				+ enabled + "\" >\n");
		sb.append("<description>");
			if(jd.getDescription() != null) sb.append(jd.getDescription());
		sb.append("</description>\n");
		for(Trigger t : s.getTriggersOfJob(jd.getName(), jd.getGroup()) )
		{
			if(t instanceof CronTrigger) {		
				CronTrigger ct = (CronTrigger) t;
				sb.append("<cron>");
					sb.append(ct.getCronExpression());
				sb.append("</cron>\n");
				//we also need to find the maximum of previous fire times
				//and the minimum of next fire times, so why not do it here
				if(t.getPreviousFireTime() != null && (Previous == null || t.getPreviousFireTime().compareTo(Previous) > 0) )
					Previous = t.getPreviousFireTime();
				if(t.getNextFireTime() != null && ( Next == null || t.getNextFireTime().compareTo(Next) < 0) )
					Next = t.getNextFireTime();
			}
		}
		sb.append("<jobdetails>");
			for(String key : jd.getJobDataMap().getKeys())
			{
				sb.append("<detail key=\"" + key + "\">");
					sb.append(jd.getJobDataMap().get(key).toString());
				sb.append("</detail>\n");
			}
		sb.append("</jobdetails>\n");
		sb.append("<lastjob>");
		if(Previous != null)	
			sb.append(Previous.toString());
		sb.append("</lastjob>\n");
		sb.append("<lastjobUTC>");
		if(Previous != null)	
			sb.append(utc.format(Previous));
		sb.append("</lastjobUTC>\n");
		sb.append("<nextjob>");
			sb.append(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(Next));
		sb.append("</nextjob>\n");
		sb.append("<nextjobUTC>");
			sb.append(utc.format(Next));
		sb.append("</nextjobUTC>\n");
		sb.append("</schedule>");
	}
	
	public void removeSchedule(String jobid) throws Exception {

		if(!s.deleteJob(jobid, Scheduler.DEFAULT_GROUP)) {
			logger.error("Could not delete job");
			throw new Exception("didn't find job");		
		} else
			logger.info("Schedule removed, id=" + jobid);

	}
	
	/**
	 * Adds details from the passed map into the job details object if they are not reserved names.
	 * Reserved names are scheduleraction, jobid, cron, jobenabled, jobdescription
	 * @param map
	 * @param details
	 */
	private void addDetails(MultivaluedMap<String,String> map, JobDetail details) {

		for(Entry<String, List<String>> en : map.entrySet()) {
			
			//the following are used for scheduling and should not be passed into jobs
			if( en.getKey().equals("scheduleraction") || 
				en.getKey().equals("jobid") ||
				en.getKey().equals("cron") || 
				en.getKey().equals("jobenabled") || 
				en.getKey().equals("jobdescription"))
				continue;
			
			details.getJobDataMap().put(en.getKey(), en.getValue().get(0));
			//logger.info(en.getKey() + "=" + en.getValue());
			System.out.println(en.getKey() + "=" + en.getValue());
		}
	}
	
	/**
	 * Add the details from a ModifyRequest object into a job detail object.
	 * @param params ModifyRequest object
	 * @param details
	 */
	private void addDetails(ModifyRequest params, JobDetail details) {
			details.getJobDataMap().putAll(params.getJobdetails());
	}

	public String doSchedule(String jobid, MultivaluedMap<String, String> params) throws Exception {
		String cron = params.getFirst("cron");
		
		try
		{
			if(cron == null)
			{
				logger.info("no cron passed, attempting to trigger job now");
				s.triggerJob(jobid, Scheduler.DEFAULT_GROUP);
				logger.info("job triggered");
				return null;
			}
			else
			{
				//here we create a new job detail and schedule it
				
				//if they passed in a retry number we should store that in the jobdetail map
				String retry = params.getFirst("retry");

				logger.info("scheduling a retry with cron=" + cron + " retry=" + retry);
				String name = jobid + "DL" + cron.replace(' ', '1').replace('?', 'A');
				JobDetail detail = new JobDetail(name,Scheduler.DEFAULT_GROUP, scheduler.jobs.WSJob.class);
				addDetails(params, detail);
				
				Trigger t = new CronTrigger("retry" + retry, "doshedulesfor=" + name, cron);

				t.getJobDataMap().put("retry", retry);
				
				s.scheduleJob(detail, t);
				s.resumeTrigger(t.getName(), t.getGroup());
				s.resumeJob(jobid, Scheduler.DEFAULT_GROUP); // is this correct???? this is the old jobdetail we are resuming??
				logger.info("scheduled successfully, triggername = " + t.getName() + " current time is : " + (new Date()) + " next fire time is: " + t.getNextFireTime());
				logger.info("trigger endtime=" + t.getFinalFireTime() + " final=" + t.getFinalFireTime());
				logger.info("trigger state =" + s.getTriggerState(t.getName(), t.getGroup()) + " scheduler in standby=" + s.isInStandbyMode() + " is started=" + s.isStarted());

				return name;
			}
		} catch (Exception e)
		{
			logger.error("Could not trigger job", e);
			throw e;
		}
	}

	public void modifySchedule(String jobid, String xml) throws Exception {

		logger.debug("xml=" + xml);
		
		ModifyRequest params = new ModifyRequest(xml); 
		logger.debug(params.toString());
		
		String enabled = params.getJobenabled();
		if(enabled != null) logger.info("enabled=" + enabled);

		//we should have a cron parameter and some number of other parameters		
		List<String> crons = params.getCrons();
		
		if( crons == null || crons.size() == 0)
		{
			if("false".equals(enabled))
			{
				disableTriggers(jobid, enabled);
				return;
			} else
			{
				logger.error("request did not have a cron field");
				throw new Exception("no cron field, and not disabling");
			}
		}

		logger.info("time is" + (new Date()));

		String description = params.getDescription();
		if(description != null) logger.info("description=" + description);

		//extract the start time and convert to a date
		String beginjobat = params.getBeginjobat();
		Date starttime = null;
		boolean triggerNow = false;
		if(beginjobat != null)
		{
			try
			{
				Format cronformatter = new SimpleDateFormat("0 mm HH dd MM ? yyyy");
				starttime = (Date)cronformatter.parseObject(beginjobat);
				logger.info("got beginjobat, date is:" + starttime.toString());
				if(starttime.before(new Date())) {
					triggerNow = true;
					starttime = null;
				}
			} catch (Exception e)
			{
				logger.error("beginjobat malformed", e);
				throw e;
			}
		} else logger.info("no beginjobat");

		//////////////////////schedule job ////////////////////////////////
		logger.info("scheduling the job");	
		try 
		{
			//first check if that schedule already exists
			Trigger trigger = s.getTrigger(jobid,Scheduler.DEFAULT_GROUP);
			JobDetail olddetails = s.getJobDetail(jobid, Scheduler.DEFAULT_GROUP);
			
			//create the details for our job and add it to the scheduler
			JobDetail details = new JobDetail(jobid, Scheduler.DEFAULT_GROUP, scheduler.jobs.WSJob.class);
			if(description != null)
			{
				logger.info("setting description: " + description);
				details.setDescription(description);
			}
			
			addDetails(params, details);

			//if the job already exists, remove it.
			if( olddetails != null)	
			{
				s.pauseJob(jobid, Scheduler.DEFAULT_GROUP);
				s.deleteJob(jobid, Scheduler.DEFAULT_GROUP);
			}

			s.addJob(details, true);

			int cronnum = 0;
			for(String cron : crons)
			{
				if(cron.equals(""))
				{
					logger.info("empty cron, scheduling for now");
					trigger = new SimpleTrigger(String.valueOf(cronnum), details.getName(), new Date());
				}
				else
				{
					logger.info("creating trigger: id=" + cronnum + " |group=" + details.getName() + " |cron=" + cron);
					trigger = new CronTrigger(String.valueOf(cronnum), details.getName(), cron);
				}
				trigger.setJobName(jobid);
				trigger.setJobGroup(Scheduler.DEFAULT_GROUP);
				if(starttime != null)
					trigger.setStartTime(starttime);
				
				//put each of the extra parameters into the datamap
				s.scheduleJob(trigger);

				if(enabled != null && enabled.equals("false"))
					s.pauseTrigger(trigger.getName(), trigger.getGroup());
				else {
					s.resumeTrigger(trigger.getName(), trigger.getGroup());
				}
				
				cronnum++;
			}
			logger.info("Job with id=" + jobid + " and " + crons.size() + " crons scheduled successfully");
			
			//if we get a begin at date in the past, we just trigger stuff for now
			if(triggerNow) {
				logger.info("triggering imediately since begin at date was in the past");
				SimpleTrigger nowT = new SimpleTrigger("now Trigger", Scheduler.DEFAULT_GROUP, new Date());
				nowT.setJobName(jobid);
				nowT.setJobGroup(Scheduler.DEFAULT_GROUP);
				s.scheduleJob(nowT);
			}

		} catch (Exception e) {
			logger.error("unable to schedule job", e);
		}
		
	}
	
	
	private void disableTriggers(String jobid, String enabled) throws Exception {
		logger.info("pausing triggers and such");
		try
		{
			JobDetail jd = s.getJobDetail(jobid, Scheduler.DEFAULT_GROUP);
			if(jd == null)
				throw new Exception("null job detail");
			
			//disabled the trigger
			for(Trigger t : s.getTriggersOfJob(jd.getName(), jd.getGroup()) )
					s.pauseTrigger(t.getName(), t.getGroup());
			
			jd.getJobDataMap().put("jobenabled", enabled);
		} catch (Exception e)
		{
			logger.error("Error changing enabled state:", e);
			throw e;			
		}
		logger.info("finished disabling");
	}
	
}
