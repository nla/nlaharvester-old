package harvester.processor.exceptions;

/**
 * Abstract exception that is used for exceptions thrown in processing steps.
 * @author adefazio
 *
 */
public abstract class CustomException extends Exception {

	//private static final long serialVersionUID = 1L;
	//private static String statusMessage = "Failed";

	public abstract String getStatusMessage();
	
}
