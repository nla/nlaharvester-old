package harvester.processor.steps;

import harvester.processor.exceptions.HarvestException;
import harvester.processor.main.Records;
import harvester.processor.util.HTMLHelper;
import harvester.processor.util.SitemapClient;
import harvester.processor.util.StepLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

/**
 * Main class for harvesting of RSS Feeds.
 * Written to work with RSS 2.0
 * 
 * @author tingram
 *
 */
public class RssHarvest implements StagePluginInterface {
	
	private Integer stepid;
	private String baseUrl;
	private boolean onlyHarvestFirst50Records;	
	private StepLogger logger;
	private String rssFeedXML;		
	private ArrayList<String> linksList; // Contains the URL's to pages to harvest used as a queue	
	public int RECORD_LIMIT = 100;
	
	/**
	 * Does not do anything
	 */
	public void Dispose() {
	}
	
	/**
	 * Initializes the class and reads the robots.txt to get the first URL to harvest.
	 * Called once.
	 */
	public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
		
		this.logger = logger;		
		this.linksList = new ArrayList<String>();
		this.rssFeedXML = "";
		this.onlyHarvestFirst50Records = false;
		this.stepid = (Integer)props.get("stepid");
		
		this.baseUrl = (props.get("URL") == null ? null : props.get("URL").toString());        
        this.onlyHarvestFirst50Records = "true".equals(props.get("until50"));
        
        //String from = (props.get("harvestfrom") == null ? null : props.get("harvestfrom").toString());
        //String until = (props.get("harvestuntil") == null ? null : props.get("harvestuntil").toString());
        
        //String metadata_prefix = props.get("Metadata Prefix").toString();
        //String record_oai_id = (props.get("singlerecord") == null ? null : props.get("singlerecord").toString());

        //forced_encoding = (String) props.get("Encoding");
        //boolean get_record = "true".equals(props.get("stopatfirst"));
        
        // Log stuff
        logger.log("URL: " +  baseUrl);
        logger.logprop("URL: ", baseUrl, stepid);
        if(onlyHarvestFirst50Records == true) logger.logprop("Stop at 50 records", "True", stepid);
        
        rssFeedXML = HTMLHelper.downloadPage(baseUrl);
        linksList.addAll(readFeedAndGatherListOfLinks(rssFeedXML));        
	}
	
	/**
	 * The main method that is called to process the sitemaps. 
	 */
	public Records Process(Records records) throws Exception {		
		
		if (linksList.isEmpty()) {
			records.setContinue_harvesting(false);
			return records;
		} else { 				
			// Harvest the actual pages		
			harvestHTMLPages(records);
		}
		
		logger.locallog("Finished Process returning records.", getName());
		return records;
	}
		
	/**
	 * Splits the response into <item> components and return the urls in the <link>
	 * element as a list.
	 *  
	 * @param response The XML document representing a RSS Feed XML Document
	 * @return ArrayList<String>
	 */
	public ArrayList<String> readFeedAndGatherListOfLinks(String response) throws HarvestException {
		
		ArrayList<String> list = new ArrayList<String>();
		
		Matcher matcher;
		Pattern item_tag = Pattern.compile("<\\s*item(\\s*|\\s.*?)>|<\\s*/\\s*item\\s*>");
		int count = 0;
		int parse_error_count = 0;

	    String xml = "";
	    while (true) {
	        int level = 0;
	        int start = -1;
	        /* Now splits <item> by finding and counting opening and closing <item> tags. */
	        matcher = item_tag.matcher(response);
	        while (matcher.find()) {
	            if (matcher.group(0).matches("<\\s*/\\s*item\\s*>")) {
	                level-=1;
	            } else {
	                level+=1;
	            }
	            if (start == -1) {
	                start = matcher.end();
	            }
	            if (level == 0) {	
	                xml = "<item>" + response.substring(start, matcher.start()) + "</item>";            	                
	                response = response.substring(matcher.end());
	                // TODO
	                // Remove this step if I am not going to use to the dates for harvesting
	                // as the XML processing adds more overhead. rather just use the regexps.
	                try {	                	
	                    Document record = DocumentHelper.parseText(xml);
	                    String link = record.selectSingleNode("//link").getText();
	                    list.add(link);
	                } catch (Exception e) {
	                    logger.logfailedrecord("Record " + count + " rejected" +
	                                           "<br/>Processing Step: " + getName() +
	                                           "<br/>Reason: Record malformed<br/>" + e.getMessage(), "Record malformed", stepid, xml);
	                    parse_error_count++;
	                }
	                	                	               	                             
	                break;
	            }
	        }
	        if (level != 0) {
	            logger.log(StepLogger.STEP_ERROR, "Urlset Error<br/>Malformed XML could not be parsed", "Malformed XML", stepid, null);
	            throw new HarvestException();
	        }
	
	        if (start == -1) {
	            break;
	        }	        	       
	
	        count++;
	    }
	    
	    logger.log(count + " links in document");      
	    logger.log(parse_error_count + " <item> elements in the feed had parse errors");
	    
		return list;
	}
	
	/**
	 * Iterates through the linksList and harvests the html pages, 
	 * cleans the HTML and adds the page as a dom4j document to the 
	 * records object.
	 * 
	 * @param records 
	 */
	public void harvestHTMLPages(Records records) throws HarvestException {
		
		int count = 0;
		int parse_error_count = 0;

	    while (true) {
	    	
	    	 if (onlyHarvestFirst50Records && count >= 50) {
	    		 records.setContinue_harvesting(false);
	             logger.locallog("Harvested 50 records, breaking from record loop", getName());
	             break;
	         }
	    	 
	    	// Don't download any more records if the upper limit has been reached or
	    	// there are no more pages to download.
	    	if (count == RECORD_LIMIT || linksList.isEmpty()) {
	    		logger.locallog("Harvested: " + count + " records, remaining url queue: " + linksList.size() + ", breaking from record loop", getName());
	    		break;
	    	}
	    	
	    	count++;
	        
	    	String url = linksList.remove(0);
	    	logger.locallog("Harvesting: " + url , getName());
	    	try {
	    		String html = HTMLHelper.downloadPage(url);
	    		org.w3c.dom.Document domDocument = HTMLHelper.tidyHtmlAndReturnAsDocument(html);
	    		org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
	    		Document doc = reader.read(domDocument);	    		 
	    		doc.setDocType(null);
	    		records.addRecord(doc);
	    	} catch (UnsupportedEncodingException e) {
	    		logger.logfailedrecord("URL: " + url + " rejected" +
                        "<br/>Processing Step: " + getName() +
                        "<br/>Reason: Unable to convert HTML into XML<br/>" + e.getMessage(), "Encoding Exception", stepid, url);
	    		parse_error_count++;
	    	}
	    	catch (IOException e) {
	    		logger.logfailedrecord("URL: " + url + " rejected" +
                        "<br/>Processing Step: " + getName() +
                        "<br/>Reason: Unable to download page<br/>" + e.getMessage(), "Connection Exception", stepid, url);
	    		logger.locallog(e.toString(), getName());
	    		parse_error_count++;
			} catch (Exception e) {
	    		logger.logfailedrecord("URL: " + url + " rejected" +
                        "<br/>Processing Step: " + getName() +
                        "<br/>Reason: <br/>" + e.getMessage(), "Unknown Exception", stepid, url);
	    		logger.locallog(e.toString(), getName());
	    		parse_error_count++;
			}     
	    	
	    	logger.locallog("Harvested " + count + " records, remaining url queue: " + linksList.size(), getName());
	    }
	    
	    logger.locallog(count + " records in batch", getName());
        logger.locallog(parse_error_count + " records in batch had parse errors", getName());
        logger.locallog(records.getCurrentrecords() + parse_error_count + " total records", getName());
        
        records.setTotalRecords(records.getCurrentrecords() + parse_error_count);
	}
	
	public String getName() {
		return "RssHarvest";
	}

	/**
	 * Gets the current size of the list of allowable URLs to process
	 */
	public int getPosition() {
		return linksList.size();
	}

	/**
	 * Not used
	 */
	public void setPosition(int arg0) {
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}  

	public ArrayList<String> getLinks() {
		return linksList;
	}

	public void setLinks(ArrayList<String> links) {
		this.linksList = links;
	}	
	
}
