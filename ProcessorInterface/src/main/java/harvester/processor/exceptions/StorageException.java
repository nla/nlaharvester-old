package harvester.processor.exceptions;

/** 
 * Should be thrown when:
 * "a harvest failed because there was a problem storing records in the Collection data store"
 * Results in a harvest status of "Failed to store"
 * @author adefazio
 *
 */
public class StorageException extends CustomException {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public String getStatusMessage() {
		return "Failed to store";
	}
}
