package harvester.processor.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.w3c.tidy.Tidy;

/**
 * Helper class with general HTML related things.
 * 
 * @author tingram
 *
 */
public class HTMLHelper {
	
	
	/**
	 * Downloads a page from a given URL and returns the page as a String.
	 * @param pageUrl
	 * @return A string representing the downloaded contents of the page.
	 * @throws IOException 
	 */
    public static String downloadPage(URL pageUrl) throws IOException {
        
        // Open connection to URL for reading.
        BufferedReader reader = new BufferedReader(new InputStreamReader(pageUrl.openStream()));
        
        // Read page into buffer.
        String line;
        StringBuffer pageBuffer = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            pageBuffer.append(line);
        }
        
        return pageBuffer.toString();
    }
    
    /**
     * Converts the html into well formed Xml.
     * Uses JTidy to clean up the html.
     * 
     * @param html
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String cleanHtmlToXml(String html) throws UnsupportedEncodingException {    	
		OutputStream out = new ByteArrayOutputStream();
		Tidy tidy = new Tidy();
		tidy.setXmlOut(true);
		tidy.parse(new ByteArrayInputStream(html.getBytes("UTF-8")), out);
		return new String(((ByteArrayOutputStream) out).toByteArray(), "UTF-8");
    }
}
