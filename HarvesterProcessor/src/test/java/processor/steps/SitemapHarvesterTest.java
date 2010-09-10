package processor.steps;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import harvester.processor.exceptions.HarvestException;
import harvester.processor.main.Records;
import harvester.processor.steps.SitemapHarvest;
import harvester.processor.test.helpers.MockServletContext;
import harvester.processor.test.helpers.MockStepLogger;

import nu.xom.Builder;

import org.dom4j.Document;
import org.junit.Before;
import org.junit.Test;


public class SitemapHarvesterTest {
	
	private SitemapHarvest sitemapHarvest;
	private String baseUrl = "http://www.google.com.au/robots.txt";
	private HashMap<String, Object> props;
	private String sitemapIndexXML;
	private String urlSetXML;
	
	@Before
	public void setup() throws Exception {
		
		// Load the test records
		Builder parser = new Builder();
		sitemapIndexXML = parser.build(getClass().getClassLoader().getResourceAsStream("sitemapindex.xml")).toXML();
		urlSetXML = parser.build(getClass().getClassLoader().getResourceAsStream("urlset.xml")).toXML();
		
		props = new HashMap<String, Object>();					
		sitemapHarvest = new SitemapHarvest();
								
		props.put("URL", baseUrl);
		props.put("stepid", 1); 
		props.put("Match URL Pattern", "http://www.nla.gov.au.*");

		sitemapHarvest.Initialise(props, new MockStepLogger(), new MockServletContext());
	}
	
	@Test
	public void shouldValidateURLPattern() throws Exception {
					
		SitemapHarvest sitemapHarvest = new SitemapHarvest();
		String result;
		
		sitemapHarvest.setAllowedURLPattern("http://www.powerhousemuseum.com/collection/database/index.php\\p{Punct}irn\\p{Punct}[\\d]+");
		result = sitemapHarvest.validateURL("http://www.powerhousemuseum.com/collection/database/index.php?irn=393338..\\.&collection=Wunderlich_Limited_Archive");
		assertEquals("http://www.powerhousemuseum.com/collection/database/index.php?irn=393338", result);
		
		sitemapHarvest.setAllowedURLPattern("http://www.powerhousemuseum.com/collection/database/index.php\\p{Punct}irn\\p{Punct}[\\d]+");
		result = sitemapHarvest.validateURL("http://www.powerhousemuseum.com/frog/database/index.php?irn=393338..\\.&collection=Wunderlich_Limited_Archive");
		assertNull(result);
		
		sitemapHarvest.setAllowedURLPattern("http://www.nla.gov.au.*");
		result = sitemapHarvest.validateURL("http://www.nla.gov.au/mappings/test");
		assertEquals("http://www.nla.gov.au/mappings/test",result);
	}
	
	@Test
	public void shouldReturnSitemapListFromRobotsTxtFile() {
		ArrayList<String> list = sitemapHarvest.readRobotsTxtAndGetSitemapList(baseUrl);
		assertTrue("Expecting at least one sitemap URL from the robots.txt file for: " + baseUrl, list.size() > 0);		
	}
	
	@Test
	public void shouldReturnListofSitemapURLs() throws HarvestException {
		ArrayList<String> list = sitemapHarvest.processSitemapIndex(sitemapIndexXML);
		assertEquals("Expected 6 URL's to be returned", 6, list.size());
	}
	
	@Test
	public void shouldReturnListofPageURLs() throws HarvestException {
		ArrayList<String> list = sitemapHarvest.processUrlSet(urlSetXML);
		assertEquals("Expected 5 URL's to be returned", 5, list.size());
	}
	
	@Test
	public void shouldEndHarvesting() throws Exception {
		sitemapHarvest.setSitemapIndexList(new ArrayList<String>());
		sitemapHarvest.setUrlsetList(new ArrayList<String>());
		Records records = new Records();
		records = sitemapHarvest.Process(records);		
		assertFalse("Expecting the continue harvesting flag to be false", records.isContinue_harvesting());
	}
		
	/**
	 * Note that this test is extremely dependent on the validity of the
	 * URL's in the input file. If this tests starts failing double check the 
	 * input url's are valid.
	 * @throws Exception
	 */
	@Test
	public void shouldHarvestPagesFromUrlSet() throws Exception {
		sitemapHarvest.setUrlsetList(sitemapHarvest.processUrlSet(urlSetXML));		
		Records records = new Records();
		sitemapHarvest.harvestHTMLPages(records);
		assertTrue("Expecting at least one HTML page to be harvested", records.getTotalRecords() > 0);
		assertTrue("Expecting to harvest 5 HTML pages", records.getTotalRecords() == 5);
	}
	
	/**
	 * Note that this test is extremely dependent on the validity of the
	 * URL's in the input file. If this tests starts failing double check the 
	 * input url's are valid.
	 * @throws Exception
	 */
	@Test
	public void shouldHarvestPagesAndCreateDom4jRecord() throws Exception {
		sitemapHarvest.setUrlsetList(sitemapHarvest.processUrlSet(urlSetXML));		
		Records records = new Records();
		sitemapHarvest.harvestHTMLPages(records);
		for (Object record : records.getRecords()) {
			try {
				Document doc = (Document)record;
			} catch (ClassCastException e) {
				fail("Expecting each record to be of type dom4j and not: " + e.getMessage());
			}
		}		
	}	

}
