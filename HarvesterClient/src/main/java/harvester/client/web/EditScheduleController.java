package harvester.client.web;

import harvester.client.data.dao.DAOFactory;
import harvester.client.schedule.*;
import harvester.client.util.*;
import harvester.data.Contributor;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class EditScheduleController implements Controller {

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
		
		logger.info("processing editSchedule request");

		int contributorid = WebUtil.getIntFieldOrError("contributorid", request);

		//get the contributor related stuff from the db
		Contributor c =  daofactory.getContributorDAO().getContributorLastHarvestsAndCollection(contributorid);
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("contributor", c);
		
		//get the schedule related stuff
		ScheduleView sv = schedulerclient.getScheduleView(c);
		
		model.put("schedule", sv);
		
		if(c.getLastsuccessfulprod() != null)
			model.put("lastsuccessfulharvest", c.getLastsuccessfulprod().getStarttime());
		
		model.put("shortnow", SchedulerClient.getNowShort());
		model.put("now", SchedulerClient.getNow());
		logger.info("now= " + SchedulerClient.getNowShort());
	
		logger.info("editschedule model built");
	    return new ModelAndView("EditSchedule", "model", model);
		    
	}
	
	
}