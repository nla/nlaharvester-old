package harvester.client.web;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.data.dao.DAOFactory;
import harvester.client.data.dao.interfaces.CollectionDAO;
import harvester.data.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ListHarvestLogsController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
		logger.info("processing list harvest logs request");
		int contributorid = 0;
		
			//parse contributorid
			contributorid = Integer.valueOf((String) request.getParameter("contributorid"));
	        
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("contributor", daofactory.getContributorDAO()
					.getContributorCollectionAndHarvests(contributorid));
	        
			logger.info("ListHarvestLogs model built");
	        return new ModelAndView("ListHarvestLogs", "model", model);
    }

}
