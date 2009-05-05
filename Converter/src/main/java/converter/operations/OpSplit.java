package converter.operations;

import org.dom4j.*;
import java.util.regex.*;
import converter.exceptions.*;

/**
 * @author Tor Lattimore
 *
 * This class splits elements by a regex create copies with split text.
 */
public class OpSplit extends Operation {
	String regex;

	/**
	 * @param regex The regex to split on
	 */
	public OpSplit(String regex) {
		this.regex = regex;
	}

	/**
	 * @param node The node to be split
	 * 
	 * An exception is thrown if the node is not an element. (It makes no sense to split elements)
	 */
	public void apply(Node node) throws Exception {
		Element element;
		System.out.println(node.getClass());
		if (node instanceof Element) {
			element = (Element)node;
		}else {
			throw new InvalidXPathTypeException("Splitting can only be done on elements, not attributes."); 
		}
		String text = element.getText();

		String[] splits;
		splits = text.split(regex);

		Element parent = element.getParent();

		element.detach();

		if (parent == null) {
			return;
		}

		for (String split : splits) {
			Element to_add = element.createCopy();
			to_add.setText(split);
			parent.add(to_add);
		}
		element.detach();
	}
}
