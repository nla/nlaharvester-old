package harvester.processor.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.w3c.tidy.Tidy;

/**
 * Helper class with general HTML related things.
 * 
 * @author tingram
 *
 */
public class HTMLHelper {
	    
    /**
     * Downloads a page from a given url and returns the page as a String.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public static String downloadPage(String url) throws IOException {
    	HttpClient client = new HttpClient();
    	HttpMethod method = new GetMethod(url);
    	method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
    	int statusCode = client.executeMethod(method);
    	String response = method.getResponseBodyAsString();
    	method.releaseConnection();
    	return response;
    }
    
    /**
     * Converts the html into well formed Xml.      
     * 
     * Uses JTidy to clean up the html.
     * 
     * @param html
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String cleanHtmlToXml(String html) throws UnsupportedEncodingException {     	
		OutputStream out = new ByteArrayOutputStream();
		Tidy tidy = new Tidy();
		tidy.setXHTML(true);
		tidy.parse(new ByteArrayInputStream(html.getBytes("UTF-8")), out);
		return new String(((ByteArrayOutputStream) out).toByteArray(), "UTF-8");
    }
    
    /**
     * Converts the html into a org.w3c.dom.Document
     * 
     * @param html
     * @return
     * @throws UnsupportedEncodingException
     */
    public static org.w3c.dom.Document tidyHtmlAndReturnAsDocument(String html) throws UnsupportedEncodingException {     	
		OutputStream out = new ByteArrayOutputStream();
		Tidy tidy = new Tidy();
		tidy.setXHTML(true);
		return tidy.parseDOM(new ByteArrayInputStream(html.getBytes("UTF-8")), out);
    }
    
    /**
     * Strip all characters before opening <html> tag.
     * @param html
     * @return
     */
    public static String stripAnythingBeforeOpeningHtmlTag(String html) {
    	return html.substring(html.indexOf("<html"));
    }
    
    /**
     * Strip and replace all &nbsp; with a space
     * 
     * @param html
     * @return
     */
    public static String replaceNbspCharactersWithSpace(String html) {
    	return html.replaceAll("&nbsp;", " ");
    }
}
