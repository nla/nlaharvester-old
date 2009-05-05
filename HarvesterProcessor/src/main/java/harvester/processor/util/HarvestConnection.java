package harvester.processor.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public interface HarvestConnection {

	public int connect(String requesturl) throws IOException;

	public InputStream getInputStream() throws IOException;
	
	public String getHeaderField(String name);
}
