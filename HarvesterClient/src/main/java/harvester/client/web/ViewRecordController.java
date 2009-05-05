package harvester.client.web;


import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;


import harvester.client.data.dao.DAOFactory;
import harvester.data.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class ViewRecordController implements Controller{
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		logger.info("processing Viewrecord request");
		Map<String, Object> model = new HashMap<String, Object>();
		
		//what type of record are we viewing, error or normal?
		String data = null;
		
		if(request.getParameter("harvestlogid") != null)
		{
			int harvestlogid = Integer.valueOf(request.getParameter("harvestlogid"));
			//extract the record value
			data = daofactory.getRecordDAO().getHarvestLogRecordData(harvestlogid);
		}
		else
		{
			int harvestdataid = Integer.valueOf(request.getParameter("harvestdataid"));
			data = daofactory.getRecordDAO().getHarvestDataRecordData(harvestdataid);
		}
		
		model.put("data", data);
		
		logger.info("viewrecord model built");
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
        return new ModelAndView("ViewRecord", "model", model);

		
	}
}