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
import org.dom4j.Element;

/**
 * Main class for harvesting of sitemaps.
 * 
 * @author tingram
 *
 */
public class SitemapHarvest implements StagePluginInterface {
	
	private Integer stepid;
	private String baseUrl;
	private String allowedURLPattern;
	private boolean onlyHarvestFirst50Records;	
	private StepLogger logger;
	private SitemapClient client;	
	private ArrayList<String> sitemapIndexList; // Contains the URL's to other sitemaps used as a queue
	private ArrayList<String> urlsetList; // Contains the URL's to pages to harvest used as a queue	
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
		this.sitemapIndexList = new ArrayList<String>();
		this.urlsetList = new ArrayList<String>();
		this.onlyHarvestFirst50Records = false;
		this.stepid = (Integer)props.get("stepid");
		
		this.baseUrl = (props.get("URL") == null ? null : props.get("URL").toString());        
        this.allowedURLPattern = (props.get("Match URL Pattern") == null ? null : props.get("Match URL Pattern").toString());
        this.onlyHarvestFirst50Records = "true".equals(props.get("until50"));
        
        //String from = (props.get("harvestfrom") == null ? null : props.get("harvestfrom").toString());
        //String until = (props.get("harvestuntil") == null ? null : props.get("harvestuntil").toString());
        
        //String metadata_prefix = props.get("Metadata Prefix").toString();
        //String record_oai_id = (props.get("singlerecord") == null ? null : props.get("singlerecord").toString());

        //forced_encoding = (String) props.get("Encoding");
        //boolean get_record = "true".equals(props.get("stopatfirst"));
        
        // Log stuff
        logger.log("URL: " +  baseUrl);
        logger.log("Allowable url pattern: " + allowedURLPattern);
        logger.logprop("URL: ", baseUrl, stepid);
        logger.logprop("Allowable url pattern: ", allowedURLPattern, stepid);
        if(onlyHarvestFirst50Records == true) logger.logprop("Stop at 50 records", "True", stepid);
       
        //logger.logprop("From", from == null ? "-" : from, stepid);
        //logger.logprop("Until", until == null ? "-" : until, stepid);
        //logger.logprop("Metadata Prefix", metadata_prefix == null ? "-" : metadata_prefix, stepid);
        //logger.logprop("Set", set == null ? "-" : set, stepid);        
        //if(record_oai_id != null) logger.logprop("identifier", record_oai_id, stepid);
                                        
        sitemapIndexList.addAll(readRobotsTxtAndGetSitemapList(baseUrl));
	}
	
	/**
	 * The main method that is called to process the sitemaps. 
	 */
	public Records Process(Records records) throws Exception {		
	
		client = new SitemapClient(logger);		
		
		if (sitemapIndexList.isEmpty() && urlsetList.isEmpty()) {
			records.setContinue_harvesting(false);
			return records;
		} 
		
		// Harvest the next sitemap 
		if (urlsetList.isEmpty()) {					
			try {			
		        logger.log("Requesting next sitemap from: " + sitemapIndexList.get(0));
		        client.makeRequest(sitemapIndexList.remove(0)); // Get and remove the first on the queue
		        String response = client.getResponse();		         
		        parseResponseAndAddUrlsToSiteIndexOrUrlsetList(response);
		    } catch (Exception e) {
		    	logger.locallog(e.toString(), getName());	            	           
		    }
		}
		
		// Harvest the actual pages
		if (!urlsetList.isEmpty()) {
			harvestHTMLPages(records);
		}
		
		logger.locallog("Finished Process returning records.", getName());
		return records;
	}
	
	/**
	 * Reads an XML response and either adds a list of URLs to the siteIndex list or
	 * the urlset list.
	 * 
	 * @param response 
	 * @throws HarvestException
	 */
	private void parseResponseAndAddUrlsToSiteIndexOrUrlsetList(String response) throws HarvestException {
		logger.locallog("Strip namespaces", getName());		
		response = stripNamespace(response);
		
		if (response.indexOf("sitemapindex") > -1) {
			logger.log("Reponse is a sitemapindex");
			sitemapIndexList.addAll(processSitemapIndex(response)); // Add the URL's to the siteMapIndexList
		} else if (response.indexOf("urlset") > -1) {
			logger.log("Reponse is a urlset");
			urlsetList.addAll(processUrlSet(response)); // Add the URL's of pages
		} else {
			logger.error("Error parsing reponse. Response not a valid sitemap document. ", null);
            throw new HarvestException();
		}
	}
	
	/** 
	 * Parses an XML response of type <sitemapindex> and retrieves all <loc> URL's and places
	 * them into a list. 
	 * @param response The XML sitemapindex of the sitemap
	 * @return A list of urls as Strings.
	 */
	public ArrayList<String> processSitemapIndex(String response) throws HarvestException {
		
		ArrayList<String> list = new ArrayList<String>();
		
		Matcher matcher;
		Pattern loc_tag = Pattern.compile("<\\s*loc(\\s*|\\s.*?)>|<\\s*/\\s*loc\\s*>");
		int count = 0;

	    String url = "";
	    while (true) {
	        int level = 0;
	        int start = -1;
	        /* Now splits <loc> by finding and counting opening and closing <loc> tags. */
	        matcher = loc_tag.matcher(response);
	        while (matcher.find()) {
	            if (matcher.group(0).matches("<\\s*/\\s*loc\\s*>")) {
	                level-=1;
	            } else {
	                level+=1;
	            }
	            if (start == -1) {
	                start = matcher.end();
	            }
	            if (level == 0) {	
	                url = response.substring(start, matcher.start());
	                list.add(url);	                
	                response = response.substring(matcher.end());
	                break;
	            }
	        }
	        if (level != 0) {
	            logger.log(StepLogger.STEP_ERROR, "Sitemap Error<br/>Malformed XML could not be parsed", "Malformed XML", stepid, null);
	            throw new HarvestException();
	        }
	
	        if (start == -1) {
	            break;
	        }
	
	        count++;
	    }
	    logger.log(count + " sitemap urls in batch");	    
	    return list;
	}
	
	/**
	 * Splits the response into <url> components and return the urls in the <loc>
	 * element as a list. An additional step involves parsing the URL to determine 
	 * whether it matches the desired URL pattern.
	 *  
	 * @param response The XML document representing a <urlset> sitemap
	 * @return ArrayList<String>
	 */
	public ArrayList<String> processUrlSet(String response) throws HarvestException {
		
		ArrayList<String> list = new ArrayList<String>();
		
		Matcher matcher;
		Pattern loc_tag = Pattern.compile("<\\s*url(\\s*|\\s.*?)>|<\\s*/\\s*url\\s*>");
		int count = 0;
		int parse_error_count = 0;

	    String xml = "";
	    while (true) {
	        int level = 0;
	        int start = -1;
	        /* Now splits <url> by finding and counting opening and closing <url> tags. */
	        matcher = loc_tag.matcher(response);
	        while (matcher.find()) {
	            if (matcher.group(0).matches("<\\s*/\\s*url\\s*>")) {
	                level-=1;
	            } else {
	                level+=1;
	            }
	            if (start == -1) {
	                start = matcher.end();
	            }
	            if (level == 0) {	
	                xml = "<url>" + response.substring(start, matcher.start()) + "</url>";            	                
	                response = response.substring(matcher.end());
	                // TODO
	                // Remove this step if I am not going to use to the dates for harvesting
	                // as the XML processing adds more overhead. rather just use the regexps.
	                try {	                	
	                    Document record = DocumentHelper.parseText(xml);
	                    String loc = record.selectSingleNode("//loc").getText();
	                    String validatedURL = validateURL(loc);
	                    //logger.locallog(loc, getName());
	                    if (validatedURL != null) list.add(validatedURL);
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
	    
	    logger.log(count + " page urls in document");      
	    logger.log(list.size() + " allowable page urls in document");
	    logger.log(parse_error_count + " <url> elements in batch had parse errors");
	    
		return list;
	}
	
	/**
	 * Tests the url against the URL pattern and returns the correct substring or
	 * null if the url does not match the pattern.
	 * 
	 * @param url
	 * @return
	 */
	public String validateURL(String url) {
		Pattern p = Pattern.compile(allowedURLPattern);		
	    Matcher matcher = p.matcher(url);
	    if (matcher.find()) {
	    	return matcher.group();
	    } else {
	    	return null;
	    }
	}
	
	/**
	 * Iterates through urlsetList and harvests the html pages, cleans them and adds
	 * the page as a dom4j document to the records.
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
	    	if (count == RECORD_LIMIT || urlsetList.isEmpty()) {
	    		logger.locallog("Harvested: " + count + " records, remaining url queue: " + urlsetList.size() + ", breaking from record loop", getName());
	    		break;
	    	}
	    	
	    	count++;
	        
	    	String url = urlsetList.remove(0);
	    	logger.locallog("Harvesting: " + url , getName());
	    	try {
	    		String html = HTMLHelper.downloadPage(url);
	    		org.w3c.dom.Document domDocument = HTMLHelper.tidyHtmlAndReturnAsDocument(html);
	    		org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
	    		Document doc = reader.read(domDocument);
	    		addIdentifierURL(doc, url);
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
	    	
	    	logger.locallog("Harvested " + count + " records, remaining url queue: " + urlsetList.size(), getName());
	    }
	    
	    logger.locallog(count + " records in batch", getName());
        logger.locallog(parse_error_count + " records in batch had parse errors", getName());
        logger.locallog(records.getCurrentrecords() + parse_error_count + " total records", getName());
        
        records.setTotalRecords(records.getCurrentrecords() + parse_error_count);
	}
	
	/**
	 * Adds a new meta tag to the HTML with the URL of the page.
	 * @param doc
	 * @param pageUrl
	 */
	private void addIdentifierURL(Document doc, String pageUrl) {
		Element root = doc.getRootElement();
		Element el = DocumentHelper.createElement("meta");
		el.addAttribute("name", "identifier.url");
		el.addAttribute("content", pageUrl);
		root.add(el);
	}
    
	/**
	 * Reads the Robots.txt file and retrieves a list of URLs to the Sitemaps.
	 */
	public ArrayList<String> readRobotsTxtAndGetSitemapList(String baseUrl) {
		
		ArrayList<String> list = new ArrayList<String>();
		
        try {
            URL robotsFileUrl = new URL(baseUrl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(robotsFileUrl.openStream()));
                        
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("Sitemap:") >= 0 || line.indexOf("sitemap") >=0) {                	
                	String sitemapURL = line;                   	
                	if (sitemapURL.indexOf("Sitemap:") >= 0) 
                		sitemapURL = sitemapURL.substring("Sitemap:".length());
                                    	                	
                    // Remove comments if present
                    int commentIndex = sitemapURL.indexOf("#");
                    if (commentIndex != - 1) 
                    	sitemapURL = sitemapURL.substring(0, commentIndex);                
                                        
                    sitemapURL = sitemapURL.trim();
                    list.add(sitemapURL);
                    logger.log("Found the Sitemap URL: " + sitemapURL);                    
                }
            }                        
        } catch (Exception e) {
        	logger.error("Error with reading the baseURL: " + baseUrl, e);
        }        
        return list;
	}
	
	private String stripNamespace(String response) {
		return response.replaceFirst("xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"", "");
	}
	
	public String getName() {
		return "SitemapHarvest";
	}

	/**
	 * Gets the current size of the list of allowable URLs to process
	 */
	public int getPosition() {
		return urlsetList.size();
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

	public ArrayList<String> getSitemapIndexList() {
		return sitemapIndexList;
	}

	public void setSitemapIndexList(ArrayList<String> sitemapIndexList) {
		this.sitemapIndexList = sitemapIndexList;
	}

	public ArrayList<String> getUrlsetList() {
		return urlsetList;
	}

	public void setUrlsetList(ArrayList<String> urlsetList) {
		this.urlsetList = urlsetList;
	}

	public String getAllowedURLPattern() {
		return allowedURLPattern;
	}

	public void setAllowedURLPattern(String allowedURLPattern) {
		this.allowedURLPattern = allowedURLPattern;
	}	
	
}
