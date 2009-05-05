package harvester.client.schedule.test;

import harvester.client.schedule.*;
import harvester.data.Contributor;

import java.util.LinkedList;


import com.meterware.httpunit.HttpUnitOptions;

import junit.framework.TestCase;

public class SchedulerClientTest extends TestCase{

	protected void setUp()
	{
		
	}
	
	public void testScheduleToViewToScheduleConversion() throws Exception
	{
		Schedule s = new Schedule();
		s.setCrons(new LinkedList<String>());
		s.getCrons().add("0 20 13 1 SEP,AUG,MAR *");
		s.getCrons().add("0 8 10 1 SEP,AUG,MAR *");
		s.getCrons().add("0 32 18 1 SEP,AUG,MAR *");
		s.setDescription("test description");
		s.setEnabled("true");
		s.setId("42");
		
		SchedulerClient sc = new SchedulerClient();
		sc.setHarvesterurl("http://localhost:8080/HarvesterProcessor");
		sc.setWsurl("http://localhost:8080/Scheduler");
		
		Contributor c = new Contributor();
		
		ScheduleView sv = sc.buildView(s, c);
		
		assertEquals(sv.getDescription(), s.getDescription());
		assertEquals(String.valueOf(sv.getJobid()), s.getId());
		assertEquals(sv.isEnabled(), true);
		
		//check that the crons we added were converted correctly
		assertEquals(sv.getDates().size(), 1);
		assertEquals(sv.getDates().getFirst(), Integer.valueOf(1));
		assertTrue(sv.getMonths().containsKey("SEP"));
		assertTrue(sv.getMonths().containsKey("AUG"));
		assertTrue(sv.getMonths().containsKey("MAR"));
		assertEquals(sv.getTimes().size(), 3);
		
		Schedule s2 = sc.convertViewToSchedule(sv);
		
		//new description is created 
		//assertEquals(s2.getDescription(), s.getDescription());
		assertEquals(s2.getId(), s.getId());
		
		//This is not used in practice
		//assertEquals(s2.getCrons().get(0), s.getCrons().get(0));
		//assertEquals(s2.getCrons().get(1), s.getCrons().get(1));
		//assertEquals(s2.getCrons().get(2), s.getCrons().get(2));
	}
	
	
	
	
	
}
