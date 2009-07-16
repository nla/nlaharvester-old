package harvester.client.harvest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HarvestQueue {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	private String harvesterurl;
	
	public Document getQueue() throws IOException, SAXException {
    	//Make a request to HarvesterProcessor for the queue
    	//parse the response as xml
    	//convert xml to json
    	
		URL requesturl = new URL(harvesterurl + "?action=list");
		logger.info("queue url: " + requesturl.toString());
		
		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
		conn.setRequestMethod("GET");
		
		conn.connect();
		if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
			logger.error("could not fetch queue");
			throw new IOException();
		}
		
        DOMParser parser = new DOMParser();

	    InputStream in = conn.getInputStream();
	    InputStreamReader isr = new InputStreamReader(in, "UTF-8");
	    InputSource source = new InputSource(isr);
	    parser.parse(source);
        in.close();

        conn.disconnect();
        Document doc = parser.getDocument();
        
        logger.info("got xml document from ws");
		
        //XPathFactory factory = XPathFactory.newInstance();
        //XPath xpath = factory.newXPath();
        return doc;
	}
	
	public String getHarvesterurl() {
		return harvesterurl;
	}

	public void setHarvesterurl(String harvesterurl) {
		this.harvesterurl = harvesterurl;
	}
}
