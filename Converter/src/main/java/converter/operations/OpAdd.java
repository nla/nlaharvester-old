package converter.operations;

import org.dom4j.*;
import converter.transform.*;
import converter.*;

/**
 * @author Tor Lattimore
 * Not a functioning operation. Not currently used by the AddStep
 */
public class OpAdd extends Operation {

	String name;
	String vt;

	public OpAdd(String name, String vt) {
		this.name = name;
		this.vt = vt;
	}

	public void apply(Element element) throws Exception {
		element.addElement(name).setText(new Transform(element.getDocument()).convert(".", ".", vt, element));
	}
}
