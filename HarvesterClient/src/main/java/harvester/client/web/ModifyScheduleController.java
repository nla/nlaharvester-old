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


public class ModifyScheduleController implements Controller {

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
			HttpServletResponse responce) throws Exception {
		
		logger.info("processing ModifySchedule request");
		//get all the passed fields we are expecting
		int contributorid = Integer.valueOf((String) request.getParameter("contributorid"));
		
		//best to do database stuff first
		Contributor c = daofactory.getContributorDAO()
			.getContributorCollectionAndDataprofile(contributorid);
		
		//we can approach this by converting the passed parameters into a schedule view object
		ScheduleView sv = new ScheduleView();
		
		if(request.getParameter("enabled") != null)
			sv.setEnabled(true);
		if("weekly".equals(request.getParameter("period")) )
			sv.setWeekly(true);
		if(request.getParameter("delete") != null)
			sv.setDelete(true);
		
		//days
		try
		{
			String[] days = request.getParameterValues("day");
			sv.setDays(new HashMap<String, Boolean>());
			for(String day : days)
				sv.getDays().put(day, true);
		}
		catch (Exception e)
		{
			//there was probably no days ticked
		}
		
		//dates and times
		sv.setDates(new LinkedList<Integer>());
		sv.setTimes(new LinkedList<ViewTime>());
		int biggesttime = Integer.valueOf(request.getParameter("biggesttime"));
		int biggestdate = Integer.valueOf(request.getParameter("biggestdate"));
		//there can be gaps in the numbering, if the user deletes a time in the middle of the list for example
		//whatever they do biggesttime is an upper bound for the indexes of all the times they entered
		for(int i=1; i <= biggesttime; i++)
		{
			String hour = request.getParameter("hour" + i);
			String minute = request.getParameter("minute" + i);
			
			if(hour != null && minute != null)
			{
				sv.getTimes().add(new ViewTime(Integer.valueOf(hour), Integer.valueOf(minute)));
			}
		}
		for(int i=1; i <= biggestdate; i++)
		{
			String date = request.getParameter("date" + i);
			
			if(date != null)
			{
				logger.info("date found:" + date);
				sv.getDates().add(Integer.valueOf(date));
			}
		}
		
		//months
		try
		{
			String[] months = request.getParameterValues("month");
			sv.setMonths(new HashMap<String, Boolean>());
			for(String month : months)
				sv.getMonths().put(month, true);
		}
		catch (Exception e)
		{
			//there was probably no months ticked
		}
		
		//from and begin
		if(request.getParameter("from") != null)
			sv.setFrom(Integer.valueOf(request.getParameter("from")));

		sv.setFromdate(request.getParameter("fromdate"));
		logger.info("from=" + sv.getFromdate());
		if(request.getParameter("begin") != null)
			sv.setBegin(Integer.valueOf(request.getParameter("begin")));
		sv.setBegindate(request.getParameter("begindate"));
		
		
		sv.setJobid(contributorid);
		
		int oldisscheduled = c.getIsscheduled();
		Integer oldfirst = c.getIsfinishedfirstharvest();
		schedulerclient.makeScheduleModifications(sv, c);
		
		//if the from field was changed we need to update the db as well
		if(sv.getFrom() == sv.FROM_SET || c.getIsscheduled() != oldisscheduled || oldfirst != c.getIsfinishedfirstharvest())
		{
			logger.info("updating contributor");
			daofactory.getContributorDAO().updateContributor(c);
		}
		
		logger.info("modification complete");
		//now that we are done redirect to the viewHarvestSchedule page
		RedirectView rv = new RedirectView("ViewContributor.htm?contributorid=" + contributorid);
		ModelAndView mv = new ModelAndView(rv);
		return mv;
	}
	
	
}