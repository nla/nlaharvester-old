package harvester.processor.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * This class enables easy access to download and unzip sitemaps.
 * Refer to: http://en.wikipedia.org/wiki/Sitemaps
 * 
 * @author tingram
 *
 */
public class SitemapClient {
	
	StepLogger logger;
	private HarvestConnection con;
	private String response;
	
	public SitemapClient(StepLogger logger) {
		this.logger = logger;
	}
	
	private String getName() {
		return getClass().getName();
	}
	
	public void makeRequest(String url) {
		
		ByteArrayOutputStream rawout = new ByteArrayOutputStream();				
		Integer responseCode = null;
		InputStream responseStream = null;
		con = new HarvestConnectionImpl();
		
		try {
			responseCode = con.connect(url);			
			logger.locallog("Response Code: " + responseCode, getName());			
		} catch(IOException e) {
			logger.error("error while trying to connect to or get the input stream from the repository", e);
			responseCode = null;
		}
		
		try {
			
			String contentEncoding = con.getHeaderField("Content-Type");
			logger.locallog("Encoding: " + contentEncoding, getName());
			
			if ("application/x-gzip".equals(contentEncoding)) {
				logger.locallog("content is GZIP compressed", getName());
				responseStream = new GZIPInputStream(con.getInputStream());					
			} else {
				logger.locallog("content is normal", getName());
				responseStream = con.getInputStream();
			}
			
			byte[] b = new byte[4096];
			for (int n; (n = responseStream.read(b)) != -1;) {
				rawout.write(b, 0, n);
			}						
			
		} catch (IOException e) {
			logger.error("error with given input stream from repository", e);
		}	
		
		response = decode(rawout.toByteArray());
	}
	
	private String decode(byte[] data) {
		
		logger.locallog("response was " + data.length + " bytes long", getName());
		String encoding = "UTF-8";
		String response = "";
		
		try {
			response = new String(data, encoding);
		} catch (Exception e) {
			logger.locallog("failed to encode document. Document not re-encoded", getName());
			try {response = new String(data, "UTF-8"); } catch (Exception e2) {}		
		}
		return response;
	}

	public String getResponse() {
		return response;
	}	
	
	
	
}
