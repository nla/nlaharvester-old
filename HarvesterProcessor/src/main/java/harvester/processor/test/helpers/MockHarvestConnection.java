package harvester.processor.test.helpers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import harvester.processor.util.HarvestConnection;

/**
 * An object for mocking web service requests
 * @author adefazio
 *
 */
public class MockHarvestConnection implements HarvestConnection {

	public Queue<byte[]> records = new LinkedList<byte[]>();
	public HashMap<String, String> headers = new HashMap<String, String>(); 
	
	private int responseCode = 200;
	
	public void setResponseCode(int code) {
		responseCode = code;
	}
	
	public void addRecord(byte[] recorddata) {
		records.add(recorddata);
	}
	
	public void addHeader(String name, String value) {
		headers.put(name, value);	
	}
	
	public int connect(String requesturl) throws IOException {
		return responseCode;
	}

	public String getHeaderField(String name) {
		return headers.get(name);
	}

	public InputStream getInputStream() throws IOException {
		if(records.size() > 0)
			return new ByteArrayInputStream(records.remove());
		else
			return new ByteArrayInputStream("overflowed queue".getBytes());
	}

}
