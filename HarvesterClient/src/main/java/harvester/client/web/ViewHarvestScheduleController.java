package harvester.client.web;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.data.dao.*;
import harvester.client.schedule.*;
import harvester.data.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ViewHarvestScheduleController implements Controller {

    private SchedulerClient schedulerclient;

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
            throws Exception {
        
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
		DateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		utc.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		logger.info("processing viewHarvestSchedule request");
		//get all the passed fields we are expecting
		int contributorid = Integer.valueOf((String) request.getParameter("contributorid"));

		//get the contributor related stuff from the db
		Contributor c = daofactory.getContributorDAO().getContributorCollectionContactsAndLastHarvest(contributorid);
		
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("contributor", c);
		
		//get the schedule related stuff
		Schedule schedule = schedulerclient.getSchedule(String.valueOf(contributorid));
		if(schedule != null) {
			
			Date last = daofactory.getContributorDAO().getLastProductionHarvestDate(contributorid);
			//we need to massage the last scheduled dates abit for the view's benefit
			if("".equals(schedule.getLast()))
					schedule.setLast(null);
			if(last != null) {
				schedule.setLast(df.format(last));
				schedule.setLastUTC(utc.format(last));
			}
			if(c.getLastsuccessfulprod() != null) {
				schedule.setLastSuccessful(df.format(c.getLastsuccessfulprod().getStarttime()));
				schedule.setLastSuccessfulUTC(utc.format(c.getLastsuccessfulprod().getStarttime()));
			}
			model.put("scheduleview", schedulerclient.buildView(schedule, c));
			
			logger.info("last utc : " + schedule.getLastUTC());
		}
		model.put("schedule", schedule);
			
    	logger.info("viewHarvestSchedule model built");
        return new ModelAndView("ViewHarvestSchedule", "model", model);
		
    }

}
