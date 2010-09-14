package contributors;

import harvester.processor.main.Records;
import harvester.processor.steps.RssHarvest;
import harvester.processor.test.helpers.MockServletContext;
import harvester.processor.test.helpers.MockStepLogger;
import harvester.processor.util.HTMLHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This class is specifically for testing the ABC Radio National as a contributor.
 * 
 * A simple way to test their data.
 * Usually used for debugging purposes and most tests will be set to Ignore as
 * this is not to be used in the build lifecycle.
 * 
 * @author tingram
 *
 */
public class ABCRadioNationalTests {
	
	
	@Ignore
	@Test
	public void shouldHarvestAllInTheMindFeed() throws Exception {
				
		HashMap<String, Object> props = new HashMap<String, Object>();													
		props.put("URL", "http://www.abc.net.au/rn/allinthemind/rss/aim.xml");
		props.put("stepid", 1);		
		
		RssHarvest rssHarvest = new RssHarvest();
		rssHarvest.Initialise(props, new MockStepLogger(), new MockServletContext());
		rssHarvest.RECORD_LIMIT = 10;
		
		Records records = new Records(); 		
		int count = 0;
		
		while (count < rssHarvest.RECORD_LIMIT) {
			count++;						
			records = new Records();
			records.setContinue_harvesting(true);
			rssHarvest.Process(records);		
		} 					
	}
	
	//@Ignore
	@Test
	public void shouldDownloadAndCleanPage() throws MalformedURLException, IOException, DocumentException {
		String page = HTMLHelper.downloadPage("http://abc.net.au/rn/allinthemind/stories/2010/3004743.htm");					
		org.w3c.dom.Document domDoc = HTMLHelper.tidyHtmlAndReturnAsDocument(page);
		org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
		org.dom4j.Document doc = reader.read(domDoc);
		doc.setDocType(null);		
		System.out.println(doc.asXML());					
	}
	
	
	@Ignore
	@Test
	public void shouldDownloadAndConvertPageIntoDom4jDocument() throws MalformedURLException, IOException, DocumentException {
		String page = HTMLHelper.downloadPage("http://abc.net.au/rn/allinthemind/stories/2010/3004743.htm");			
		org.w3c.dom.Document domDoc = HTMLHelper.tidyHtmlAndReturnAsDocument(page);
		org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
		org.dom4j.Document doc = reader.read(domDoc);
		doc.setDocType(null);
		System.out.println(doc.asXML());
		Document d = DocumentHelper.parseText(doc.asXML());	
	}
	
}
