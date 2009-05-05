package converter.operations;

import converter.*;
import org.dom4j.*;

/**
 * @author Tor Lattimore
 *
 * A very simple operation. Simply detaches the node
 */
public class OpDelete extends Operation {
	public void apply(Node element) {
		element.detach();
	}
}
