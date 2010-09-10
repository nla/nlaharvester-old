package contributors;

import harvester.processor.main.Records;
import harvester.processor.steps.SitemapHarvest;
import harvester.processor.test.helpers.MockServletContext;
import harvester.processor.test.helpers.MockStepLogger;
import harvester.processor.util.HTMLHelper;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This class is specifically for testing the PowerhouseMusuem as a contributor.
 * A simple way to test their data.
 * Usually used for debugging purposes and most tests will be set to Ignore as
 * this is not to be used in the build lifecycle.
 * 
 * @author tingram
 *
 */
public class PowerhouseMuseumTests {
	
	/**
	 * This testcase is just to test the PowerHouse Museum as a contributor.
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void shouldHarvestPowerHouseMusuemRecords() throws Exception {
				
		HashMap<String, Object> props = new HashMap<String, Object>();													
		props.put("Base URL", "http://www.powerhousemuseum.com/robots.txt");
		props.put("stepid", 1);
		props.put("urlPattern", "http://www.powerhousemuseum.com/collection/database/index.php\\p{Punct}irn\\p{Punct}[\\d]+");
		
		SitemapHarvest sitemapHarvest = new SitemapHarvest();
		sitemapHarvest.Initialise(props, new MockStepLogger(), new MockServletContext());
		sitemapHarvest.RECORD_LIMIT = 10;
		
		Records records = new Records(); 		
		int count = 0;
		
		while (count <= 2) {
			count++;						
			records = new Records();
			records.setContinue_harvesting(true);
			sitemapHarvest.Process(records);		
		} 					
	}
	
	@Ignore
	@Test
	public void shouldDownloadAndCleanPage() throws MalformedURLException, IOException, DocumentException {
		String page = HTMLHelper.downloadPage("http://www.powerhousemuseum.com/collection/database/index.php?irn=27713");
					
		org.w3c.dom.Document domDoc = HTMLHelper.tidyHtmlAndReturnAsDocument(page);
		org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
		org.dom4j.Document doc = reader.read(domDoc);
		doc.setDocType(null);
		
		System.out.println(doc.asXML());					
	}
	
	
	@Ignore
	@Test
	public void shouldConvertPageIntoDom4jDocument() throws MalformedURLException, IOException, DocumentException {
		String page = HTMLHelper.downloadPage("http://www.powerhousemuseum.com/collection/database/index.php?irn=502789");
		
		//String html = HTMLHelper.cleanHtmlToXml(page);		
		//html = HTMLHelper.stripAnythingBeforeOpeningHtmlTag(html);
		//html = HTMLHelper.replaceNbspCharactersWithSpace(html);		
		//Document doc = DocumentHelper.parseText(html);		
		//System.out.println(html);			
		
		org.w3c.dom.Document domDoc = HTMLHelper.tidyHtmlAndReturnAsDocument(page);
		org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
		org.dom4j.Document doc = reader.read(domDoc);
		doc.setDocType(null);
		System.out.println(doc.asXML());
		
		OutputFormat format = OutputFormat.createPrettyPrint();
		StringWriter out = new StringWriter();
		format.setEncoding("UTF-8");
		XMLWriter writer = new XMLWriter( out, format );
		writer.write((Document)doc);
		writer.close();
		out.close();
		String data = out.toString();
		
		//Document d = DocumentHelper.parseText(doc.asXML());	
		
		
	}
	
}
