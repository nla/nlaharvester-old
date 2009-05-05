package harvester.client.web;
import harvester.client.data.dao.DAOFactory;
import harvester.client.schedule.Schedule;
import harvester.client.schedule.ScheduleView;
import harvester.client.schedule.SchedulerClient;
import harvester.data.Contributor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;



public class EditManualHarvestController implements Controller {

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
		
		logger.info("processing Edit manual harvest request");
		//get all the passed fields we are expecting
		int contributorid = Integer.valueOf((String) request.getParameter("contributorid"));
		
		//get the contributor related stuff from the db
		Map<String, Object> model = new HashMap<String, Object>();
		Contributor c = daofactory.getContributorDAO()
			.getContributorLastHarvestsAndCollection(contributorid);
		model.put("contributor", c);
		
		if(c.getLastsuccessfultest() != null)
			model.put("lastsuccessfulharvest", DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(c.getLastsuccessfultest().getStarttime()));
		else
			model.put("lastsuccessfulharvest", "None");
	
		model.put("shortnow", SchedulerClient.getNowShort());
		model.put("now", SchedulerClient.getNow());
		logger.info("now= " + SchedulerClient.getNowShort());
		
		//get the schedule for any currently scheduled test harvest, if there is one
		//ScheduleView sv = schedulerclient.getScheduleView(c);
		//model.put("schedule", sv);
		
		
		logger.info("Edit manual harvest model built");
	    return new ModelAndView("EditManualHarvest", "model", model);
		
	}

}
