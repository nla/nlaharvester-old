package harvester.processor.exceptions;

public class ConnectionException extends Exception{

	private int responseCode = 0;
	
	public ConnectionException(String msg, int code) {
		super(msg);
		responseCode = code;
	}
	
	public int getResponseCode() {
		return responseCode;
	}
	
}
