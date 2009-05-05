package scheduler;

import org.junit.*;
import java.util.*;

import javax.ws.rs.core.MultivaluedMap;

import org.quartz.*;

//import com.sun.jersey.impl.MultivaluedMapImpl;

//import com.sun.tools.javac.util.List;

import scheduler.ModifyRequest;
import scheduler.SchedulerService;
import static org.junit.Assert.*;

public class SchedulerServiceTest {

	
	
    String schedulexml = "<schedule id=\"1003\" enabled=\"true\">"
        + "<description>At 2:0 on the 4th, 1st of JAN,MAR</description>"
        + "<cron>0 0 2 4,1 JAN,MAR ? *</cron>"
        + "<jobdetails>"
        + "<detail key=\"action\">start</detail>"
        + "<detail key=\"type\">1</detail>"
        + " <detail key=\"contributorid\">1003</detail>"
        + "<detail key=\"url\">http://localhost:8080/HarvesterProcessor</detail>"
        + "</jobdetails>"
        + "</schedule>";
	
	private String desc = "At 2:0 on the 4th, 1st of JAN,MAR";
    private String cron = "0 0 2 4,1 JAN,MAR ? *";
	
	public Scheduler createTestScheduler() throws Exception {
		//create 3 threaded scheduler without using properties file.
		org.quartz.impl.DirectSchedulerFactory.getInstance().createVolatileScheduler(3);
		Scheduler sched = org.quartz.impl.DirectSchedulerFactory.getInstance().getScheduler();
		
		System.out.println(sched.getSchedulerName());
		sched.start();
		return sched;
	}
	
	
	@Test public void buildAndRetrieveTest() throws Exception {
		
		Scheduler s = createTestScheduler();
		SchedulerService sv = new SchedulerService(s);
		
		//send a request and then see if we get what we expect scheduled.
		sv.modifySchedule("1003", schedulexml);
		JobDetail jd = s.getJobDetail("1003", Scheduler.DEFAULT_GROUP);
		JobDataMap map = jd.getJobDataMap();
		Trigger[] ts = s.getTriggersOfJob("1003", Scheduler.DEFAULT_GROUP);
		assert(jd != null);
		assert(map != null);
		assert(jd.getDescription().equals(desc));
		assert(map.get("action").equals("start"));
		assert(map.get("contributorid").equals("1003"));
		assert(ts.length == 1);	
		
		s.shutdown();
		
	}
	
	@Test public void getAndDeleteScheduleTest() throws Exception {
		
		Scheduler s = createTestScheduler();
		SchedulerService sv = new SchedulerService(s);
		
		sv.modifySchedule("1003", schedulexml);
		LinkedList<String> ids = new LinkedList<String>();
		ids.add("1003");
		String resultxml = sv.getschedule(ids);
		
		//convert result into a ModifyRequest object and compare to the request we originally sent
		//result is wrapped in a schedules tag, we need to remove it
		resultxml = resultxml.substring("<?xml version=\"1.0\"?><schedules>".length() , resultxml.length() - "</schedules>".length());
		//System.out.println(resultxml);
		
		ModifyRequest mr = new ModifyRequest(resultxml);		
		assert(mr.getJobid().equals("1003"));
		assert(mr.getDescription().equals(desc));
		assert(mr.getJobenabled().equals("true"));
		assert(mr.getJobdetails().get("url").equals("http://localhost:8080/HarvesterProcessor"));
		assert(mr.getCrons().contains(cron));
		
		//now remove the schedule
		
		sv.removeSchedule("1003");
		try {
			String result = sv.getschedule(ids);
			System.out.println("returned result after delete");
			System.out.println(result);
			assert(false);
		} catch (Exception e) {
			assert(true);
		}
		
		JobDetail jd2 = s.getJobDetail("1003", Scheduler.DEFAULT_GROUP);
		assert(jd2 == null);
		
		s.shutdown();
	}
	
	@Test public void doScheduleTest() throws Exception {
		//do schedule request allow you to add an extra cron, unconnected to the original
		//we also test scheduling a cron that will go off later, and test that the schedluler picks it up.
		Scheduler s = createTestScheduler();
		SchedulerService sv = new SchedulerService(s);
		
		sv.modifySchedule("1003", schedulexml);
		//import com.sun.ws.rest.impl.MultivaluedMapImpl;
		MultivaluedMap<String, String> params = new com.sun.jersey.core.util.MultivaluedMapImpl();
		params.add("cron", "0 0 3 1 JAN ? *");
		params.add("fish", "sticks");
		
		String name = sv.doSchedule("1003", params);
		
		Trigger[] ts = s.getTriggersOfJob(name, Scheduler.DEFAULT_GROUP);
		assert(ts.length == 1);
		
		System.out.println(ts[0].getNextFireTime().toString());
		assert(ts[0].getNextFireTime().toString().matches(".* Jan 01 03:00:00 .*"));
		JobDataMap jdm = s.getJobDetail(name, Scheduler.DEFAULT_GROUP).getJobDataMap();
		System.out.println(jdm.keySet());
		assert(jdm.get("fish").equals("sticks"));
		
		s.shutdown();
	}
	
	
}
