package harvester.client.web;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.data.dao.DAOFactory;
import harvester.client.data.dao.interfaces.CollectionDAO;
import harvester.client.harvest.HarvestError;
import harvester.client.util.KeyValue;
import harvester.client.util.WebUtil;
import harvester.data.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ViewHarvestController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

		logger.info("processing viewHarvest request");

		int harvestid = 0;
		//parse harvestid
		harvestid = Integer.valueOf((String) request.getParameter("harvestid"));

		Map<String, Object> model = new HashMap<String, Object>();
		Harvest harvest = daofactory.getHarvestDAO().getHarvestContributorCollection(harvestid);
		model.put("harvest", harvest);
		model.put("contributor", harvest.getContributor());
		model.put("dateformat", new WebUtil());
		List<Object> logs = daofactory.getRecordDAO().getHarvestLogs(harvestid);
		List<KeyValue> props = new LinkedList<KeyValue>();
		boolean hasClusters = daofactory.getHarvestDAO().hasClusters(harvestid);

		//we need to do some processing on the logs to remove any logs marked as prop log messages

		for(Iterator<Object> itor = logs.iterator(); itor.hasNext() ;) {
			HarvestLog log = (HarvestLog) itor.next();
			if(log.getErrorlevel() == HarvestLog.PROP_INFO) {
				try {
					String[] parts = log.getDescription().split("=");
					props.add(new KeyValue(parts[0], parts[1]));
					itor.remove();						
				} catch (Exception e) {
					logger.info("problem parsing a prop info log message. desc=" + log.getDescription());
				}
			}
			if(log.getErrorlevel() == HarvestLog.REPORT_INFO) {				
				itor.remove(); //don't show report info		
			}

		}
	
		List<HarvestError> errors = daofactory.getHarvestDAO().getErrorSummary(harvestid);
		model.put("errors", errors);
		
		model.put("props", props);
		model.put("logs", logs);					
		model.put("hasClusters", hasClusters);
		
		try
		{
			long duration = harvest.getEndtime().getTime() - harvest.getStarttime().getTime();	        
			model.put("duration", duration / 1000);
		} catch (Exception e)
		{
			model.put("duration", 0);
		}

		logger.info("viewHarvest model built");
		return new ModelAndView("ViewHarvest", "model", model);
	}

}
