package harvester.client.web;

import harvester.client.data.dao.DAOFactory;
import harvester.client.harvest.HarvestInfo;
import harvester.client.harvest.HarvestQueue;
import harvester.client.schedule.Schedule;
import harvester.client.schedule.SchedulerClient;
import harvester.client.service.CollectionService;
import harvester.client.util.WebUtil;
import harvester.data.Contributor;
import harvester.data.Harvest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.xml.xpath.XPath;
import org.json.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Controller
//@SuppressWarnings("unchecked")
public class HarvestRESTController {

	protected final Log logger = LogFactory.getLog(getClass());
    
    private HarvestQueue harvest_queue;
    private SchedulerClient schedulerclient;
    private DAOFactory daofactory;
    private CollectionService collectionService;

    private DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);

    @RequestMapping("/GetPast.json")
    public void getPast(@RequestParam("collectionid") int collectionid, 
    					 HttpServletResponse response) throws Exception {  
    	
    	JSONObject categories = new JSONObject();
    	
    	//factor out categories stuff from ListHarvests controller
    	//work out what to do with sorting code
    	//fill categories object based off of HarvestNameModel object
    	
		logger.info("returning GetPast response");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(categories.toString());
    }
    
    @RequestMapping("/GetRunning.json")
    public void getRunning(@RequestParam("collectionid") int collectionid, 
    					 HttpServletResponse response) 
    throws JSONException, IOException, SAXException, XPathExpressionException {
    	
    	List<Harvest> harvests = collectionService.getRunningHarvests(collectionid); 	
    	
        JSONArray running_array = new JSONArray();
    	
		for(Harvest h : harvests) {
			JSONObject jharvest = new JSONObject();

			Contributor c= h.getContributor();
			
			jharvest.put("harvestid", h.getHarvestid());
			jharvest.put("contributorid", c.getContributorid());
			jharvest.put("contributorname", c.getName());
			jharvest.put("status", h.getStatus());
			jharvest.put("type", c.getHtype() == Contributor.OAI ? "OAI" : "Z39.50");
			jharvest.put("time", WebUtil.formatFuzzyDate(h.getStarttime()));
			jharvest.put("unixtime", h.getStarttime().getTime());
			
			int rejected = h.getTotalrecords()-h.getRecordscompleted();
			int total = h.getTotalrecords();
			float rejectedpercentage = (float) 0.0;
			
			if(rejected != 0 && total != 0)
				rejectedpercentage = ((float)rejected / total)*100;
			jharvest.put("recordsrejected", rejected);
			jharvest.put("rejectedpercentage", rejectedpercentage);
			jharvest.put("goodpercentage", 100 - rejectedpercentage);
			jharvest.put("recordcount", total);
			
			running_array.put(jharvest);
		}

    	JSONObject jqueue = new JSONObject();
    	jqueue.put("running", running_array);
    	
		logger.info("returning GetRunning response");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jqueue.toString());
    }
    
    @RequestMapping("/GetQueue.json")
    public void getQueue(@RequestParam("collectionid") int collectionid, 
    					 HttpServletResponse response) 
    throws JSONException, IOException, SAXException, XPathExpressionException {
    	
    	Document doc = harvest_queue.getQueue();
    	
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
    	
        NodeList harvests = (NodeList)xpath.evaluate("//harvest[@collectionid=" + collectionid + "]", doc, XPathConstants.NODESET);
        
        JSONArray harvest_array = new JSONArray();
        
        for(int i=0; i < harvests.getLength(); i++) {
        	Node harvest = harvests.item(i);
        	int contributorid = Integer.parseInt(xpath.evaluate("@contributorid", harvest));
        	int type = Integer.parseInt(xpath.evaluate("@type", harvest));
        	String position = xpath.evaluate("@position", harvest);
        	String contributorname = daofactory.getContributorDAO().getContributor(contributorid).getName();
        	
        	JSONObject harvest_object = new JSONObject();
        	harvest_object.put("position", position);
        	harvest_object.put("contributorname", contributorname);
        	harvest_object.put("contributorid", contributorid);
        	harvest_object.put("type", type == Contributor.OAI ? "OAI" : "Z39.50");
        	harvest_array.put(harvest_object);
        }
        
    	JSONObject jqueue = new JSONObject();
    	jqueue.put("queue", harvest_array);
    	
		logger.info("returning GetQueue response");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jqueue.toString());
    }
    
    @RequestMapping("/GetSchedules.json")
    public void getSchedules(@RequestParam("collectionid") int collectionid, 
    					     HttpServletResponse response) 
    throws JSONException, IOException, SAXException, XPathExpressionException  {
    	HashMap<String, Contributor> cons = collectionService.getScheduledContributors(collectionid);
    	    	
    	
        JSONArray schedule_array = new JSONArray();
    	
		if(cons.size()!= 0) {
			try {
				//getting all the schedules in one request is important for performance
				List<String> cids = new LinkedList<String>();
				cids.addAll(cons.keySet());
				List<Schedule> schedules = schedulerclient.getSchedule(cids);
				
				if(schedules != null)
					for(Schedule s : schedules) {
						JSONObject jschedule = new JSONObject();
						
						if(s.getId().endsWith("TEST")) {
							jschedule.put("test", "true");
						} 
								
						Contributor con = cons.get(s.getId());
						logger.info("considering schedule sid=" + s.getId() + " next=" + s.getNext() + "name=" + con.getName());
						
						jschedule.put("contributorid", con.getContributorid());
						jschedule.put("contributorname", con.getName());
						jschedule.put("type", con.getHtype() == Contributor.OAI ? "OAI" : "Z39.50");
						jschedule.put("unixtime", df.parse(s.getNext()).getTime());
						jschedule.put("time", s.getNext());
						
						if("true".equals(s.getEnabled()))
							jschedule.put("statuscode", HarvestInfo.SCHEDULED_ENABLED);
						else
							jschedule.put("statuscode", HarvestInfo.SCHEDULED_DISABLED);

						schedule_array.put(jschedule);
					}
			} catch (Exception e) {
				logger.error("Problem loading schedules",e);
			}
		}
		
    	JSONObject jqueue = new JSONObject();
    	jqueue.put("scheduled", schedule_array);
    	
		logger.info("returning GetQueue response");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jqueue.toString());
		
    }
    
    @Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
    
    @Autowired
	public void setHarvest_queue(HarvestQueue harvest_queue) {
		this.harvest_queue = harvest_queue;
	}
    
    @Autowired
	public void setSchedulerclient(SchedulerClient schedulerclient) {
		this.schedulerclient = schedulerclient;
	}
    
    @Autowired
	public void setCollectionService(CollectionService collectionService) {
		this.collectionService = collectionService;
	}
}
