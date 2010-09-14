package processor.steps;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import harvester.processor.exceptions.HarvestException;
import harvester.processor.main.Records;
import harvester.processor.steps.RssHarvest;
import harvester.processor.test.helpers.MockServletContext;
import harvester.processor.test.helpers.MockStepLogger;

import nu.xom.Builder;

import org.dom4j.Document;
import org.dom4j.Node;
import org.junit.Before;
import org.junit.Test;


public class RssHarvestTest {
	
	private RssHarvest rssHarvester;
	private String baseUrl = "http://news.google.com.au/news?output=rss";
	private HashMap<String, Object> props;
	private String rssFeedXml;
	
	@Before
	public void setup() throws Exception {
		
		// Load the test records
		Builder parser = new Builder();
		rssFeedXml = parser.build(getClass().getClassLoader().getResourceAsStream("rssfeed.xml")).toXML();
		
		props = new HashMap<String, Object>();					
		rssHarvester = new RssHarvest();
								
		props.put("URL", baseUrl);
		props.put("stepid", 1); 

		rssHarvester.Initialise(props, new MockStepLogger(), new MockServletContext());
	}
		
	@Test
	public void shouldReturnListOfLinksFromFeed() throws HarvestException {
		ArrayList<String> list = rssHarvester.readFeedAndGatherListOfLinks(rssFeedXml);
		assertEquals("Expecting 7 url's in the list from the rssfeed.xml file", 7, list.size());	
	}
	
	
	@Test
	public void shouldEndHarvesting() throws Exception {
		rssHarvester.setLinks(new ArrayList<String>());
		Records records = new Records();
		records = rssHarvester.Process(records);		
		
		assertFalse("Expecting the continue harvesting flag to be false", records.isContinue_harvesting());
	}
	
		
	
	@Test
	public void shouldHarvestHTMLPagesFromLinksList() throws Exception {
		ArrayList<String> links = new ArrayList<String>();		
		// Add a few test pages
		links.add("http://www.nla.gov.au/find");
		links.add("http://www.nla.gov.au/services");
		links.add("http://www.nla.gov.au/library");
		rssHarvester.setLinks(links);				
		Records records = new Records();
		rssHarvester.harvestHTMLPages(records);
		
		assertTrue("Expecting at least one HTML page to be harvested", records.getTotalRecords() > 0);
		assertTrue("Expecting to harvest 3 HTML pages", records.getTotalRecords() == 3);
	}		
	
	@Test
	public void shouldIncludePageUrlAsMetaElement() throws Exception {
		ArrayList<String> links = new ArrayList<String>();				
		links.add("http://www.nla.gov.au/find");		
		rssHarvester.setLinks(links);				
		Records records = new Records();
		rssHarvester.harvestHTMLPages(records);				
		assertTrue("Expecting to harvest 1 HTML pages", records.getTotalRecords() == 1);
		Document doc = (Document)records.getRecords().get(0);
		//System.out.println(doc.asXML());
		Node n = doc.selectSingleNode("//meta[@name='identifier.url']/@content");
		assertEquals("Expecting to find the page url as a meta tag", "http://www.nla.gov.au/find", n.getText());
	}		

}
