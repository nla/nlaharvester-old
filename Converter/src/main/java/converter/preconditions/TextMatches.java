package converter.preconditions;

import org.dom4j.*;

/**
 * @author Tor Lattimore
 *
 * Only returns true if the text matches a regular expression
 */
public class TextMatches extends Precondition {
	String regex;
	
	/**
	 * @param regex represents the regex which must match the text.
	 */
	public TextMatches(String regex) {
		if (regex == null || regex.equals("")) {
			regex = ".*";
		}
		this.regex = regex;
	}
	public boolean matches(String text) {
		return text.matches(regex);
	}
}
