package harvester.processor.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.junit.Assert;
import org.junit.Test;

public class HTMLHelperTest {
	
	@Test
	public void shouldReturnHTMLPageFromURL() throws Exception {
		String page = HTMLHelper.downloadPage(new URL("http://www.nla.gov.au"));
		assertNotNull(page);
		assertTrue(page.contains("National Library Of Australia"));
		assertTrue(page.contains("<html"));
	}
	
	@Test
	public void shouldCleanUpBadHtml() throws UnsupportedEncodingException {		
		String badHtml = "<html><div></html>";
		String goodXml = HTMLHelper.cleanHtmlToXml(badHtml);
		
		try {
			DocumentHelper.parseText(goodXml);
		} catch (DocumentException e) {
			Assert.fail("Unable to clean the html document. \n" + e);
		}
	}
}
