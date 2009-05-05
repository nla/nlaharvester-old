package harvester.client.web;

import org.omg.CORBA.TRANSACTION_REQUIRED;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.data.dao.DAOFactory;
import harvester.client.data.dao.interfaces.CollectionDAO;
import harvester.client.schedule.*;
import harvester.client.util.KeyValue;
import harvester.client.util.ViewTime;
import harvester.data.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ModifyManualHarvestController implements Controller {

	private static Integer BEGIN_NOW = 1;
	private static Integer BEGIN_AT_GIVEN_DATE = 42;
	
	private static Integer FROM_LAST_SUCC = 1;
	private static Integer FROM_EARLIEST = 2;
	private static Integer FROM_GIVEN_DATE = 42;
	
	private static Integer UNTIL_LAST_RECORD = 1;
	private static Integer UNTIL_GIVEN_DATE = 42;
	private static Integer UNTIL_50 = 2;
	
	private static Integer DELETE_NONE = 1;
	private static Integer DELETE_NOT_HARVESTED = 2;
	private static Integer DELETE_ALL = 3;
	
	
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

    private SchedulerClient schedulerclient;

	public SchedulerClient getSchedulerclient() {
		return schedulerclient;
	}

	public void setSchedulerclient(SchedulerClient schedulerclient) {
		this.schedulerclient = schedulerclient;
	}

	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		logger.info("processing modify manual harvest request");
		//get all the passed fields we are expecting
		int contributorid = 0;
		String from;
		String until;
		String fromdate;
		String untildate;
		String begin;
		String begindate;
		String delete;
		String records;
		String recordid;
		int fromEarliest = 0;

		contributorid = Integer.valueOf((String) request.getParameter("contributorid"));
		from = request.getParameter("from");
		until = request.getParameter("until");
		fromdate = request.getParameter("fromdate");
		untildate = request.getParameter("untildate");
		begin = request.getParameter("begin");
		begindate = request.getParameter("begindate");
		delete = request.getParameter("delete");
		records = request.getParameter("records");
		recordid = request.getParameter("recordid");

		//needed by the scheduler client so it knows what its actually scheduling
		Contributor c = daofactory.getContributorDAO()
			.getContributorCollectionAndDataprofile(contributorid);
		
		//we need to set up our values to pass to scheduler client
		//this just involves nulling things that do not have there
		//radio button checked
		
		if(FROM_EARLIEST.toString().equals(from))
		{
			fromdate = null;
			fromEarliest = SchedulerClient.FROM_EARLIEST;
		} else if(FROM_LAST_SUCC.toString().equals(from))
		{
			fromdate = null;
			fromEarliest = SchedulerClient.FROM_LAST_SUCC;
		} else
		{
			fromEarliest = SchedulerClient.FROM_GIVEN_DATE;
		}
		
		if(BEGIN_NOW.toString().equals(begin))
			begindate = null;
		
		if(UNTIL_LAST_RECORD.toString().equals(until) || UNTIL_50.toString().equals(until))
			untildate = null;
		
		boolean until50 = false;
		if(UNTIL_50.toString().equals(until))
			until50=true;
		
		if(DELETE_NONE.toString().equals(delete))
			delete = "none";
		else if(DELETE_NOT_HARVESTED.toString().equals(delete))
			delete = "notharvested";
		else if(DELETE_ALL.toString().equals(delete))
			delete = "all";
		
		//(String harvestdate, int fromtype, String from, String until, contributor c, boolean test, boolean until50, String delete)
		
		if("all".equals(records))
			schedulerclient.runManualHarvest(begindate, fromEarliest, fromdate, untildate, until50, delete, c);
		else 
			schedulerclient.runManualHarvest(recordid.trim(), c);
		
		logger.info("Manual harvest request processing complete");
		//now that we are done redirect to the viewHarvestSchedule page
		
		//this is a horrible way to do this, generally time based things get outdated when hardware is updated
		//this amount is low enough that it probably won't be noticed.
		Thread.sleep(1000);	//sleep for 1 second to give the processor time to start the harvest
		RedirectView rv = new RedirectView("ListHarvests.htm?collectionid=" + c.getCollection().getCollectionid());
		ModelAndView mv = new ModelAndView(rv);
		return mv;
	}
}
