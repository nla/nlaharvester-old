package converter.operations;

import org.dom4j.*;
import converter.transform.*;
import converter.*;


/**
 * @author Tor Lattimore
 *
 * This class renames an element.
 */
public class OpRename extends Operation {
	String name;

	/**
	 * Accepts a name
	 */
	public OpRename(String name) {
		this.name = name;
	}
	/**
	 * Application only applies to elements.
	 */
	public void apply(Node element) throws Exception {
		element.setName(name);
	}
}
