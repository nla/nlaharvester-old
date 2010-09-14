package harvester.processor.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class HTMLHelperTest {
	
	@Test
	public void shouldReturnHTMLPageFromURL() throws Exception {
		String page = HTMLHelper.downloadPage("http://www.nla.gov.au");
		assertNotNull(page);
		assertTrue(page.contains("National Library Of Australia"));
		assertTrue(page.contains("<html"));
	}
		
}
