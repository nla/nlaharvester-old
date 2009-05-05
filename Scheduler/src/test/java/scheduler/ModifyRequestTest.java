package scheduler;

import org.junit.*;

import scheduler.ModifyRequest;
import static org.junit.Assert.*;

public class ModifyRequestTest {
    
    String schedulexml = "<schedule id=\"1003\" enabled=\"true\">"
                        + "<description>At 17:0,4:0,3:15,2:0 on the 4th, 1st of JAN,MAR</description>"
                        + "<cron>0 15 3 4,1 JAN,MAR ? *</cron>"
                        + "<cron>0 0 17 4,1 JAN,MAR ? *</cron>"
                        + "<cron>0 0 4 4,1 JAN,MAR ? *</cron>"
                        + "<cron>0 0 2 4,1 JAN,MAR ? *</cron>"
                        + "<jobdetails>"
                        + "<detail key=\"action\">start</detail>"
                        + "<detail key=\"type\">1</detail>"
                        + " <detail key=\"contributorid\">1003</detail>"
                        + "<detail key=\"from\">1970-01-01T00:00:00Z</detail>"
                        + "<detail key=\"profileid\">1007</detail>"
                        + "<detail key=\"url\">http://localhost:8080/HarvesterProcessor</detail>"
                        + "<detail key=\"task\">0</detail>"
                        + "</jobdetails>"
                        + "<beginjobat>fish</beginjobat>"
                        + "</schedule>";
    
    String lightschedulexml = "<schedule id=\"1003\">"
        + "<cron>0 15 3 4,1 JAN,MAR ? *</cron>"
        + "<jobdetails>"
        + "<detail key=\"action\">start</detail>"
        + "<detail key=\"type\">1</detail>"
        + "</jobdetails>"
        + "</schedule>";
    
    @Test public void xmlParseTest() throws Exception {
    	
    	System.out.println("xml Parse Test");
    	ModifyRequest rq = new ModifyRequest(schedulexml);
    	
    	assert(rq != null); 
    	assert(rq.getJobid().equals("1003"));
    	assert(rq.getBeginjobat().equals("fish"));
    	assert(rq.getJobdetails().size() == 7);
    	assert(rq.getJobdetails().get("action").equals("start"));
    	
    	rq = new ModifyRequest(lightschedulexml);
    	assert(rq != null);
    	assert(rq.getCrons().contains("0 15 3 4,1 JAN,MAR ? *"));
    	assert(rq.getBeginjobat() == null);
    	assert(rq.getDescription() == null);
    }
    
    
}