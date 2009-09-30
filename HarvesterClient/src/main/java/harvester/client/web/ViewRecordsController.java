package harvester.client.web;


import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import harvester.client.data.dao.DAOFactory;
import harvester.client.util.HarvestDataView;
import harvester.data.*;
import harvester.data.Collection;

import javax.servlet.http.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class ViewRecordsController implements Controller{
	
    private int pagesize; 
    private Resource casualLayoutStylePath;
    private Resource xmlLayoutStylePath;



	public Resource getCasualLayoutStylePath() {
		return casualLayoutStylePath;
	}

	public void setCasualLayoutStylePath(Resource casualLayoutStylePath) {
		this.casualLayoutStylePath = casualLayoutStylePath;
	}

	public Resource getXmlLayoutStylePath() {
		return xmlLayoutStylePath;
	}

	public void setXmlLayoutStylePath(Resource xmlLayoutStylePath) {
		this.xmlLayoutStylePath = xmlLayoutStylePath;
	}

	public int getPagesize() {
		return pagesize;
	}

    @Required
	public void setPagesize(int pagesize) {
		this.pagesize = pagesize;
	}
	
	
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		logger.info("processing Viewrecords request");
		Map<String, Object> model = new HashMap<String, Object>();

		//we should be passed a harvest id
		Integer harvestid = Integer.valueOf(request.getParameter("harvestid"));
		
		if(harvestid == null)
		{
		}
		
		Harvest h = daofactory.getHarvestDAO().getHarvestAndContributor(harvestid);
		Contributor c = h.getContributor();
		Collection col = daofactory.getCollectionDAO().getCollectionAndProfiles(c.getCollection().getCollectionid());
		
		//add the contributor and the harvest to the model
		model.put("contributor", c);
		model.put("harvest", h);
		
		int page = Integer.valueOf(request.getParameter("page"));
		
		List<HarvestDataView> rows = daofactory.getHarvestDAO().getHarvestDataWithPaging(page, harvestid, pagesize);
		
		//we need to run the style sheet over each before displaying
		TransformerFactory xformFactory = TransformerFactory.newInstance();
		//Resource xslfile = new ClassPathResource(stylepath);
		
		Resource styleRes = null;
		String style = request.getParameter("style");
		if(style == null || "casual".equals(style)) {
			//check if there is a stylesheet specifically for this type of load step
			try {
				if(col.getLoadstage() != null) {
					Integer stepid = col.getLoadstage().getStep().getStepid();
					styleRes = new ClassPathResource("load_step_style_" + stepid + ".xsl");
				}
			} catch (Exception e) {
			}
			if(styleRes == null || !styleRes.exists())
				styleRes = casualLayoutStylePath;
		} else {
			styleRes = xmlLayoutStylePath;
			model.put("style", "xml");
		}
		
		Source xsl = new StreamSource(styleRes.getInputStream());
		//Source xsl = new StreamSource(stylepath);
		
		Transformer stylesheet = xformFactory.newTransformer(xsl);
		
		for(HarvestDataView row : rows) {
			try {

				  Source Trequest  = new StreamSource(new StringReader(row.getData()));
				  StringWriter out = new StringWriter();
				  Result Tresponse = new StreamResult(out);
				  stylesheet.transform(Trequest, Tresponse);
				  
				  row.setData(out.toString());
				}
				catch (TransformerException e) {
				  logger.error("error transforming data", e);
				}
		}
		
		model.put("rows", rows);
		model.put("pagenumber", page);
		int totalrecords = daofactory.getHarvestDAO().getTotalDataRecords(harvestid);
		//without this, this shows an extra page when total records is a multiple of pagesize
		if(totalrecords % pagesize == 0 && totalrecords != 0)
			totalrecords -= 1;
		model.put("totalpages", (totalrecords)/pagesize );	
		
		logger.info("viewrecords model built");
        return new ModelAndView("ViewRecords", "model", model);

		
	}
}