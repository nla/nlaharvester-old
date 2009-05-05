package converter.operations;

import org.dom4j.*;
import converter.transform.*;
import converter.*;

/**
 * @author Tor Lattimore
 *
 * This class is just an abstraction on the Transform class wrapped into an Operation.
 */
public class OpConvert extends Operation {
	String matcher;
	String vt;

	boolean matched;
	public boolean getMatched() {
		return matched;
	}

	/**
	 * Takes a regex and a velocity template
	 */
	public OpConvert(String matcher, String vt) {
		if (matcher == null || matcher.equals("")) {
			matcher = ".*";
		}

		this.matcher = matcher;
		this.vt = vt;
	}

	/**
	 * Applied to a node
	 */
	public void apply(Node element) throws Exception {
		matched = false;
		Transform t = new Transform(element.getDocument());
		element.setText(t.convert(element.getText(), matcher, vt, element));
		if (t.getMatched()) matched = true;
	}

}
