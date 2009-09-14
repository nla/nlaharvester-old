package harvester.client.connconfig.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import harvester.client.util.*;
import harvester.data.ProfileStep;

import java.io.*;
import java.net.*;
import java.util.List;

/**
 * This class performs the deletion of production records from a arrow datastore
 * it is linked to the datastore by the servletcontext xml document.
 */
public class ArrowLoaderActions implements LoadStepActions {

    private static Log logger = LogFactory.getLog(ArrowLoaderActions.class);
	private String contributorname;

    private String solrurl;
     
	public String getSolrurl() {
		return solrurl;
	}
	public void setSolrurl(String solrurl) {
		this.solrurl = solrurl;
	}
	
    private String arrowurl;

	public String getArrowurl() {
		return arrowurl;
	}
	public void setArrowurl(String arrowurl) {
		this.arrowurl = arrowurl;
	}
	public void deleteProductionRecords(int contributorid, String contributorname) {
		
		ArrowLoaderRun run = new ArrowLoaderRun();
		this.contributorname = contributorname;
		logger.info("starting new thread to run task");
		//we want this to run async, so we start a new thread to run the task on
		new Thread(run).start();
		logger.info("thread started, returning to normal execution");
		
	}

	public class ArrowLoaderRun implements Runnable
	{

		public void run() {
			logger.info("new runnable ArrowLoaderRun task called in a new thread");
			logger.info("ArrowLoaderActions called with url: " + arrowurl);
			try {
				callService(arrowurl + "/main/delete_institution?name=" + URLEncoder.encode(contributorname, "UTF-8"));
			}catch (Exception e) {
				logger.info("URLEncoder failed");
			}
		}

		public void postString(String str) {
			try {		
				SimplePostTool tool	= new SimplePostTool(new URL(getSolrurl() + "/update"));
				StringReader reader = new StringReader(str);
				StringWriter writer = new StringWriter();
				tool.postData(reader, writer);
			}catch (Exception e) {
				logger.error("could not build url");
			}
		}
		
		public void callService(String str) {
			try {
				URL url = new URL(str);
				URLConnection c = url.openConnection();
				c.getInputStream().close();
			} catch (Exception e) {
				logger.info("failed to connect to webservice: " + str);
			}
		}
		
	}

	/**
	 * This code is ripped from arrow loader, and may not be the best way to do this.
	 */
	public Integer getCollectionSize() {
		try {		
			HttpURLConnection c = null;
			URL url = null;
			url = new URL(getSolrurl() + "/select?" + URLEncoder.encode("fl=inst_type,contributed_date,la_created_date,la_updated_date,primary_inst,identifier,oaiid,state,inst", "UTF-8") + "&limit=1&q=" 
					+ URLEncoder.encode("*:*", "UTF-8")); 
			c = (HttpURLConnection)url.openConnection();
			c.setReadTimeout(60000);	// 1 minute
			c.setConnectTimeout(60000);  
			BufferedInputStream buffer = new BufferedInputStream(c.getInputStream());
		
			SAXReader xmlReader = new SAXReader();
			xmlReader.setValidation(false);
			Document doc = xmlReader.read(buffer);		
			
			return getSolrDocumentHits(doc);
		} catch (Exception e) {
			logger.error("could not fetch the record count: " + e.getMessage(), e);
			return null;
		}
	}
	
	public int getSolrDocumentHits(Document solr_doc) {
		if (solr_doc == null) {
			return 0;
		}
		Element element = (Element)solr_doc.selectSingleNode("response/result");
		if (element != null) {
			return Integer.parseInt(element.attribute("numFound").getValue());
		}
		return 0;
	}
	
	public List<KeyValue> getSettings(ProfileStep load_step) {
		return null;
	}		
	
}



