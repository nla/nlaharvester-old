package harvester.processor.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HarvestConnectionImpl implements HarvestConnection{

	private HttpURLConnection con = null;
	
	public int connect(String requesturl) throws IOException {
		
		URL url = new URL(requesturl);
		con = (HttpURLConnection) url.openConnection();		
		con.setInstanceFollowRedirects(false);	//we handle our own redirects
		con.setRequestProperty("User-Agent", "NLAHarvester/1.0");
		//spec rec. "gzip;q=1.0, identity;q=0.5", old="compress, gzip, identify"
		con.setRequestProperty("Accept-Encoding","gzip;q=1.0, identity;q=0.5");
		con.setReadTimeout(600000);
		con.setConnectTimeout(600000);
		
		return con.getResponseCode();		
	}

	public InputStream getInputStream() throws IOException {
		return con.getInputStream();
	}
	
	public String getHeaderField(String name) {
		return con.getHeaderField(name);
	}
	
}
