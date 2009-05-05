package harvester.processor.exceptions;

import java.util.LinkedList;

/**
 * Just a custom exception thrown by harvest steps when they want the harvest to be able
 * to be rescheduled.
 * Results in a harvest status of something like "Rescheduled to Tue 04 Mar 12:59"
 * 
 */
public class UnableToConnectException extends CustomException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LinkedList<Object> records;

	@Override
	public String getStatusMessage() {
		return "Failed to harvest";
	}
	
	public LinkedList<Object> getRecords() {
		return records;
	}

	public void setRecords(LinkedList<Object> records) {
		this.records = records;
	}
	
}
