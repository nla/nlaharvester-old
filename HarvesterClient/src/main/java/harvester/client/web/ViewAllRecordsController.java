package harvester.client.web;


import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;


import harvester.client.data.dao.DAOFactory;
import harvester.client.util.WebUtil;
import harvester.data.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class ViewAllRecordsController implements Controller{
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		logger.info("processing get all records for a harvest request");

		int harvestid = Integer.valueOf(request.getParameter("harvestid"));

		logger.info("Response buffer size: " + response.getBufferSize());
		response.setContentType("text/xml;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.addHeader("Content-Disposition", "attachment; filename=recordsfor" + harvestid + ".xml");
		ServletOutputStream outputStream = response.getOutputStream();

		daofactory.getHarvestDAO().getStreamingHarvestData(harvestid, outputStream);

		outputStream.flush();
		outputStream.close();

		return null;
	}
}