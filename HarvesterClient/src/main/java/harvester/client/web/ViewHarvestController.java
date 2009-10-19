package harvester.client.web;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import harvester.client.data.dao.DAOFactory;
import harvester.client.harvest.HarvestError;
import harvester.client.util.KeyValue;
import harvester.client.util.WebUtil;
import harvester.data.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Controller
@SuppressWarnings("unchecked")
public class ViewHarvestController {

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;
    private WebUtil wutil = new WebUtil();
    
    @Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
    
    final int MAX_LOG_BATCH_SIZE = 100;
    
    private String buildDurationString(Harvest harvest) {
		if(harvest.getStatuscode() == harvest.RUNNING() || harvest.getEndtime() != null) {
			long endtime = harvest.getEndtime() == null ? new Date().getTime() : harvest.getEndtime().getTime();
			long duration = (endtime - harvest.getStarttime().getTime()) / 1000;	   
			long seconds = duration % 60;
			long minutes = (duration/60) % 60;
			long hours = (duration/60/60);
			return String.format("%d hours %d minutes %d seconds", hours, minutes, seconds);
		} else {
			return "Did not finish";
		}
    }
    
    @RequestMapping("/GetLogRange.json")
    public void getLogRange(@RequestParam("harvestid") int harvestid, 
    					   @RequestParam("range") String range,
    					   HttpServletResponse response) 
    throws ParseException, IOException, InterruptedException, JSONException {
    
		int splitpos = range.indexOf('-');
		int start = Integer.parseInt(range.substring(0,splitpos));
		int end = Integer.parseInt(range.substring(splitpos+1));
		
		List<Object> logs = daofactory.getRecordDAO().getVisibleHarvestLogsInRange(harvestid, start, end);
    	
		JSONArray logarray = new JSONArray();
		
		for(Object logObject : logs) {
			HarvestLog log = (HarvestLog) logObject;
			JSONObject rowobject = new JSONObject();
			rowobject.append("harvestlogid", log.getHarvestlogid());
			rowobject.append("timestamp", wutil.userformat(log.getTimestamp()));
			rowobject.append("description", log.getDescription());
			rowobject.append("hasdata", log.getHasdata());
			rowobject.append("errorlevel", log.getErrorlevel());
			logarray.put(rowobject);
		}
		
		JSONObject jharvest = new JSONObject();
		jharvest.put("logs", logarray);
		
		logger.info("returning GetLogRange response");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jharvest.toString());
    }
    
    @RequestMapping("/GetNewLogs.json")
    public void getNewLogs(@RequestParam("harvestid") int harvestid, 
    					   @RequestParam(value="fromdate", required=false) String fromdatestr,
    					   HttpServletResponse response) 
    throws ParseException, IOException, InterruptedException, JSONException {
    	
    	logger.info("frromdate: " + fromdatestr);
    	
    	List<Object> logs = null;
    	
    	Harvest harvest = daofactory.getHarvestDAO().getHarvestContributorCollection(harvestid);
    	
    	if(fromdatestr == null || fromdatestr.equals("")) {
    		logs = daofactory.getRecordDAO().getHarvestLogs(harvestid);
    	} else {
        	Date fromdate = wutil.formatter.parse(fromdatestr);
    		logs = daofactory.getRecordDAO().getNewHarvestLogs(harvestid, fromdate);
    	}
    	 
		JSONArray logarray = new JSONArray();
		
		for(Iterator<Object> itor = logs.iterator(); itor.hasNext() ;) {
			HarvestLog log = (HarvestLog) itor.next();
			if(log.getErrorlevel() == HarvestLog.PROP_INFO || log.getErrorlevel() == HarvestLog.REPORT_INFO) {
					itor.remove();						
			} else {
				//logger.info("log: " + log.getDescription());
				JSONObject rowobject = new JSONObject();
				rowobject.append("harvestlogid", log.getHarvestlogid());
				rowobject.append("timestamp", wutil.userformat(log.getTimestamp()));
				rowobject.append("description", log.getDescription());
				rowobject.append("hasdata", log.getHasdata());
				rowobject.append("errorlevel", log.getErrorlevel());
				logarray.put(rowobject);
				//if(logarray.length() > MAX_LOG_BATCH_SIZE)
				//	break;
			}
		}
		
		List<HarvestError> errors = daofactory.getHarvestDAO().getErrorSummary(harvestid);
		JSONArray errorarray = new JSONArray();
		for(HarvestError error_row : errors) {
			JSONObject rowobject = new JSONObject();
			rowobject.append("stage", error_row.getStepName());
			rowobject.append("error", error_row.getError());
			rowobject.append("recordcount", error_row.getRecordCount());
			
			errorarray.put(rowobject);
		}
		
		if(logs.size() == 0) {
			Thread.sleep(3000L); //5 seconds
		}
		
		JSONObject jharvest = new JSONObject();
		jharvest.put("logs", logarray);
		jharvest.put("errors", errorarray);
		jharvest.put("totalrecords", harvest.getTotalrecords());
		jharvest.put("recordscompleted", harvest.getRecordscompleted());
		jharvest.put("type", harvest.getType());
		jharvest.put("status", harvest.getStatus());
		jharvest.put("deletionsread", harvest.getDeletionsread());
		jharvest.put("deletionsperformed", harvest.getDeletionsperformed());

		jharvest.put("duration", buildDurationString(harvest));
		
		logger.info("returning response");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jharvest.toString());
    }
    
    @RequestMapping("/ViewHarvest.htm")
	public String viewHarvest(@RequestParam("harvestid") int harvestid,  Map model)
	throws ServletException, IOException {

		logger.info("processing viewHarvest request");

		Harvest harvest = daofactory.getHarvestDAO().getHarvestContributorCollection(harvestid);
		model.put("harvest", harvest);
		model.put("contributor", harvest.getContributor());
		model.put("dateformat", wutil);
		
		List<Object> logs = daofactory.getRecordDAO().getHarvestLogs(harvestid);
		
		List<KeyValue> props = new LinkedList<KeyValue>();
		boolean hasClusters = daofactory.getHarvestDAO().hasClusters(harvestid);

		List<KeyValue> log_placeholders = new ArrayList<KeyValue>();
		List<HarvestLog> filtered_logs = new ArrayList<HarvestLog>();
		String last_date_shown = "";
		
		//we need to do some processing on the logs to remove any logs marked as prop log messages
		
		for(Iterator<Object> itor = logs.iterator(); itor.hasNext() ;) {
			HarvestLog log = (HarvestLog) itor.next();
			if(log.getErrorlevel() == HarvestLog.PROP_INFO) {
				try {
					String desc = log.getDescription();
					int splitpos = desc.indexOf('=');
					props.add(new KeyValue(desc.substring(0,splitpos), desc.substring(splitpos+1)));
					itor.remove();	
					continue;
				} catch (Exception e) {
					logger.info("problem parsing a prop info log message. desc=" + log.getDescription());
				}
			}
			if(log.getErrorlevel() == HarvestLog.REPORT_INFO) {				
				itor.remove(); //don't show report info		
				continue;
			} 
		}
		
		int count = 0;
		int current_batch_size = 0;
		int errors_in_batch = 0;
		for(Object log_object : logs) {
			HarvestLog log = (HarvestLog)log_object;
			if(logs.size() - count > MAX_LOG_BATCH_SIZE) {
				if(log.getErrorlevel() != HarvestLog.INFO) {
					errors_in_batch++;
				}
				
				if(current_batch_size == MAX_LOG_BATCH_SIZE) {
					String msg = "Log messages " + (count-current_batch_size) + " to " + count + " hidden." + 
					 			 (errors_in_batch != 0 ? "<br /> " + errors_in_batch + " errors within" : "");
					log_placeholders.add(new KeyValue((count-current_batch_size) + "-" + count, msg));
					current_batch_size = 0;
					errors_in_batch = 0;
				}
				
				current_batch_size++;
			} else {	
				if(current_batch_size != 0) {
					String msg = "Log messages " + (count-current_batch_size) + " to " + count + " hidden." + 
		 			 			 (errors_in_batch != 0 ? "<br /> " + errors_in_batch + " errors within" : "");
					log_placeholders.add(new KeyValue((count-current_batch_size) + "-" + count, msg));
					current_batch_size = 0;
				}
				filtered_logs.add(log);
			}
			count++;
		}
	
		if(filtered_logs.size() > 0)
			last_date_shown = wutil.userformat(filtered_logs.get(filtered_logs.size()-1).getTimestamp());
		// log.getTimestamp())
		
		List<HarvestError> errors = daofactory.getHarvestDAO().getErrorSummary(harvestid);
		model.put("errors", errors);
		
		model.put("props", props);
		model.put("logs", filtered_logs);	
		model.put("placeholders", log_placeholders);
		model.put("lastDateShown", last_date_shown);
		
		model.put("hasClusters", hasClusters);
		model.put("HarvestLogInfoConst", HarvestLog.INFO);
		model.put("duration", buildDurationString(harvest));
		model.put("number", new WebUtil());

		logger.info("viewHarvest model built");
		return "ViewHarvest";
	}

}
