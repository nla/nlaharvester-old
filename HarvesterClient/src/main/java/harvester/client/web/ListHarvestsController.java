package harvester.client.web;

import harvester.client.data.dao.DAOFactory;
import harvester.client.harvest.*;
import harvester.client.schedule.*;
import harvester.client.util.WebUtil;
import harvester.data.*;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.logging.*;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;


public class ListHarvestsController implements Controller{

    private SchedulerClient schedulerclient;
    private DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
    private int numberOfRecordsToShowBeforeChoppingOff = 10;
    
	public int getNumberOfRecordsToShowBeforeChoppingOff() {
		return numberOfRecordsToShowBeforeChoppingOff;
	}

	public void setNumberOfRecordsToShowBeforeChoppingOff(
			int numberOfRecordsToShowBeforeChoppingOff) {
		this.numberOfRecordsToShowBeforeChoppingOff = numberOfRecordsToShowBeforeChoppingOff;
	}

	public SchedulerClient getSchedulerclient() {
		return schedulerclient;
	}

	public void setSchedulerclient(SchedulerClient schedulerclient) {
		this.schedulerclient = schedulerclient;
	}
	
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

		logger.info("processing List Harvets request");
		int collectionid = Integer.valueOf((String) request.getParameter("collectionid"));


		Map<String, Object> model = new HashMap<String, Object>();
		
		//the comparators could be stored as a singleton, but it doesn't seem worth it to me
		HashMap<Integer, Comparator<HarvestInfo>> compmap = new HashMap<Integer, Comparator<HarvestInfo>>();
		Comparator<HarvestInfo> EarliestFirst = new HarvestComparatorDate(HarvestComparatorDate.NORMAL);
		Comparator<HarvestInfo> LatestFirst = new HarvestComparatorDate(HarvestComparatorDate.REVERSE);
		Comparator<HarvestInfo> ByName = new HarvestComparatorName();
		Comparator<HarvestInfo> ByStatus = new HarvestComparatorStatus();
		compmap.put(HarvestCategory.SORT_BY_DATE, LatestFirst);
		compmap.put(-HarvestCategory.SORT_BY_DATE, EarliestFirst);
		compmap.put(HarvestCategory.SORT_BY_NAME, ByName);
		//compmap.put(-HarvestCatogery.SORT_BY_NAME, ByName);	// we don't currently support sorting inversely for name
		compmap.put(HarvestCategory.SORT_BY_STATUS, ByStatus);
		
		HarvestNameModel harvests = new HarvestNameModel();
		
		//holds a string to append to the url so that we remember what columns are sorted for next time this code is reached
		StringBuilder sortmemory = new StringBuilder();
		
		//for each catogory we need to establish what ordering it should be shown as, and if it should be restricted to not show all
		//parameters specifying this are passed in as sort and viewall respectively.
		for(String name : harvests.getCategoryNames())
		{
			//get value from parameters
			String sort = request.getParameter("sort" + name);
			if(sort != null)
			{
				int isort = Integer.valueOf((String) sort);
//				if(isort != HarvestCategory.SORT_BY_NAME && harvests.getCategories().get(name).getDefaultdateordering() != HarvestComparatorDate.REVERSE)
//					isort = -isort;
				logger.info("for name=" + name + " sorting with isort=" + isort + " sorter= + " + (compmap.get(isort) == null ? "none" : compmap.get(isort).toString()));
				//create the tree set that enforces the ordering
				harvests.getCategories().get(name).setHarvests(new TreeSet<HarvestInfo>(compmap.get(isort)));
				harvests.getCategories().get(name).setSort(isort);
				sortmemory.append("&sort" + name + "=" + sort);
			} else
				harvests.getCategories().get(name).setHarvests(new TreeSet<HarvestInfo>(compmap.get(harvests.getCategories().get(name).getDefaultsort())));
			
			String viewall = request.getParameter("viewall" + name);
			if(viewall != null && viewall.equals("true"))
			{
				harvests.getCategories().get(name).setViewall(true);
				sortmemory.append("&viewall" + name + "=true");
			}
		}
		
		model.put("sortmemory", sortmemory.toString());
		
		//get a set of all contributors
		Collection col = daofactory.getCollectionDAO().getCollectionContributorsAndLastHarvests(collectionid);		
		Set<Contributor> contributors = col.getContributors();
		
		//Generate relevant times for comparison with harvest start times
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		Date SevenDaysAgo =  cal.getTime();
		cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -3);
		Date ThreeDaysAgo = cal.getTime();
		cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 7);	
		Date SevenDaysAhead = cal.getTime();
		
		/*
					Test Harvests(test contributors only)
					  - Contributor.type = test
					
					Production Harvests (production contributors only)
					 Waiting for a schedule
					    - isscheduled = no/null
					 From Monitored Contributors
					    - ismonitored = true
					    - lastharvestdate > 7.days.ago
					    - only show 10
					 Unsuccessful
					    - lastharvest != lastsuccessful harvest
					    - only show 10
					 Recent Successful
					    - lastharvest == lastsuccessful
					    - lastharvestdate > 3.days.ago
					Scheduled
					    - nextharvest < 7.days.from.now	
		 */
		
		for(Contributor con : contributors)
		{		
			logger.info("considering contributor, cid=" + con.getContributorid() + " name=" + con.getName());
			
			if(con.getHidefromworktray() != null && con.getHidefromworktray() == 1)
			{
				logger.info("hiding this contributor");
				continue;
			}
			
			if(con.getLastharvest() != null && con.getLastharvest().getStatuscode() == Harvest.RUNNING)
				logger.info("running");	//Don't display it here
			else if(con.getType() == 0)	//if it is a test contributor, add it to the test list
				harvests.getCategories().get("TestHarvests").getHarvests().add(toHarvestInfo(con));
			else if(con.getIsscheduled() == 0)
				harvests.getCategories().get("UnScheduled").getHarvests().add(toHarvestInfo(con));
			else if(con.getIsmonitored() == 1 && (con.getLastharvest() != null && con.getLastharvest().getStarttime().after(SevenDaysAgo)) )
				harvests.getCategories().get("Monitored").getHarvests().add(toHarvestInfo(con));
			else if( con.getLastharvest() != null && con.getLastharvest().getStatuscode() == Harvest.FAILED )
			{
				harvests.getCategories().get("Unsuccessful").getHarvests().add(toHarvestInfo(con));
			}
			else if(con.getLastharvest() != null && con.getLastharvest().getStarttime().after(ThreeDaysAgo))
				harvests.getCategories().get("RecentSuccessful").getHarvests().add(toHarvestInfo(con));
			
		}
		
		//now we need to trim any sets that are oversized
		for(String name : harvests.getCategoryNames())
		{
			HarvestCategory hc = harvests.getCategories().get(name);
			if(hc.getHarvests().size() > getNumberOfRecordsToShowBeforeChoppingOff() && !hc.isViewall())
			{
				hc.setOverflowed(true);	//so the gui know to add a ..viewmore... link
				LinkedHashSet<HarvestInfo> newset = new LinkedHashSet<HarvestInfo>();
				//we just create a new set with only the first x elements
				int i = 0;
				for(HarvestInfo hi : hc.getHarvests())
				{
					if(++i > getNumberOfRecordsToShowBeforeChoppingOff())
						break;
					newset.add(hi);
				}
				hc.setHarvests(newset);
			}
		}
		model.put("harvests", harvests);

		model.put("SORT_BY_NAME", HarvestCategory.SORT_BY_NAME);
		model.put("SORT_BY_DATE", HarvestCategory.SORT_BY_DATE);
		model.put("SORT_BY_REVERSE_DATE", HarvestCategory.SORT_BY_REVERSE_DATE);
		model.put("SORT_BY_STATUS", HarvestCategory.SORT_BY_STATUS);
		
		model.put("collection", col);
		
        logger.info("ListHarvests model built");
        return new ModelAndView("ListHarvests", "model", model);
    }

	/** basically coverters the information we have in the contributor tree into a HarvestInfo object, which is closer to the view */
	private HarvestInfo toHarvestInfo(Contributor con) {
		
		HarvestInfo hi = new HarvestInfo();

		hi.setContributorid(con.getContributorid());
		hi.setContributorname(con.getName());
		hi.setType(con.getHtype());
		if(con.getLastharvest() != null)
		{
			hi.setHarvestid(con.getLastharvest().getHarvestid());
			if(con.getLastharvest().getStarttime() != null)
			{
				hi.setTime(WebUtil.formatFuzzyDate(con.getLastharvest().getStarttime()));
				hi.setTimedate(con.getLastharvest().getStarttime());
			}
			else hi.setTimedate(null);
			
			hi.setStatus(con.getLastharvest().getStatus());
			hi.setRecordcount(con.getLastharvest().getTotalrecords());
			hi.setStatuscode(con.getLastharvest().getStatuscode());
			int rejected = con.getLastharvest().getTotalrecords()-con.getLastharvest().getRecordscompleted();
			hi.setRecordsrejected(rejected);
			float rejectedpercentage = (float) 0.0;
			if(hi.getRecordsrejected() != 0 && hi.getRecordcount() != 0)
				rejectedpercentage = ((float)hi.getRecordsrejected() / hi.getRecordcount())*100;
			
			hi.setRejectedpercentage((int)rejectedpercentage);
			hi.setGoodpercentage(100 - hi.getRejectedpercentage());
			logger.info("rejected float=" + rejectedpercentage + " int=" + hi.getRejectedpercentage());
		} else hi.setTimedate(null);	//just set it to now if it hasn't harvested yet
		
		return hi;
	}
	
	
}
