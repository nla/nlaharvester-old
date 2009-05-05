package harvester.processor.exceptions;

/**
 * To be thrown when:
 * "If the harvest failed due to a problem processing records"
 * Results in a harvest status message of "Failed to process"
 * @author adefazio
 *
 */
public class ProcessingException extends CustomException {

	private static final long serialVersionUID = 1L;

	@Override
	public String getStatusMessage() {
		return "Failed to process";
	}
}
