package harvester.processor.exceptions;

/**
 * To be thrown when:
 * "If the harvest failed because the contributor's repository gave a bad response"
 * Results in a harvest status message of "Failed to harvest" 
 * @author adefazio
 *
 */
public class HarvestException extends CustomException {

	private static final long serialVersionUID = 1L;

	@Override
	public String getStatusMessage() {
		return "Failed to harvest";
	}

}
