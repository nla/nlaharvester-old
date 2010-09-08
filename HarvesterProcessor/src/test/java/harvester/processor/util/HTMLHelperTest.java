package harvester.processor.util;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.junit.Assert;
import org.junit.Test;

public class HTMLHelperTest {
	
	@Test
	public void shouldReturnHTMLPageFromURL() throws Exception {
		String page = HTMLHelper.downloadPage("http://www.nla.gov.au");
		assertNotNull(page);
		assertTrue(page.contains("National Library Of Australia"));
		assertTrue(page.contains("<html"));
	}
		
	@Test
	public void shouldRemoveUnwantedCharactersBeforeHtml() {
		String actual = HTMLHelper.stripAnythingBeforeOpeningHtmlTag("abcd<html>");
		assertEquals("<html>", actual);
	}
	
	@Test
	public void shouldRemoveNbsp() {
		String actual = HTMLHelper.replaceNbspCharactersWithSpace("<html>&nbsp;yay&nbsp;</html>");
		assertEquals("<html> yay </html>", actual);
	}
	
	@Test
	public void shouldCleanUpBadHtml() throws UnsupportedEncodingException {		
		String badHtml = "<html><div></html>";
		String goodHtml = HTMLHelper.cleanHtmlToXml(badHtml);
		goodHtml = HTMLHelper.stripAnythingBeforeOpeningHtmlTag(goodHtml);
		
		try {
			DocumentHelper.parseText(goodHtml);
		} catch (DocumentException e) {
			Assert.fail("Unable to clean the html document. \n" + e);
		}
	}	
}
