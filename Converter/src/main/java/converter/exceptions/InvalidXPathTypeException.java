package converter.exceptions;

/**
 * @author Tor Lattimore
 *
 * This exception is use when the user supplies an xpath for an attribute when only an element xpath makes sense (eg Splitting)
 */
public class InvalidXPathTypeException extends Exception {
	public InvalidXPathTypeException(String msg) {
		super(msg);
	}
}
