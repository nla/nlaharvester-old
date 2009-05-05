package harvester.client.web;


import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.data.dao.DAOFactory;
import harvester.client.util.HarvestDataView;
import harvester.data.*;

import javax.servlet.http.*;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class ViewClustersController implements Controller{
	
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		logger.info("processing ViewClusters request");
		
		ModelAndView mv = new ModelAndView("ViewClusters");

		//we should be passed a harvest id
		Integer harvestid = Integer.valueOf(request.getParameter("harvestid"));

		Harvest h = daofactory.getHarvestDAO().getHarvestAndContributor(harvestid);
		List<HarvestCluster> hcs = daofactory.getHarvestDAO().getClusters(harvestid);
		
		int maxSize = 12;
		int minSize = 1;
		
		for(HarvestCluster hc : hcs) {
			
			int max = 0;
			int min = 0;
			
			for(HarvestClusterData hd : hc.getData()) {
				if(hd.getCount() > max)
					max = hd.getCount();
				if(hd.getCount() < min)
					min = hd.getCount();
			}
			
			int range = max-min;
			
			//we basically split the range linearly into groups and set the count of each to its group rather then its actual count
			
			float groupsize = range / (float)(maxSize - minSize);
			logger.info("SizeRange = " + (maxSize - minSize) + " range = " + range + " groupsize=" + groupsize);
			
			for(HarvestClusterData hd : hc.getData()) {
				int group = (int) ((hd.getCount() - min) / groupsize);
				hd.setCount(group + minSize);
			}
			
		}
		
		
		//add the contributor and the harvest to the model
		mv.addObject("contributor", h.getContributor());
		mv.addObject("harvest", h);
		mv.addObject("clusters", hcs);
		
		
		logger.info("viewrecords model built");
        return mv;

		
	}
}