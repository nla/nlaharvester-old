package converter;

import converter.operations.*;
import converter.preconditions.*;
import converter.exceptions.*;

import java.util.*;
import java.util.regex.*;
import org.dom4j.*;

/**
 * Filter is the main core of the Converter parsing library. The 'apply' function takes a dom4j document, an operation, an xpath and a precondition. It
 * then runs the operation against all elements in the document that match the xpath and the precondition.
 */
public class Filter {

	public static void apply(Document doc, Operation op, String xpath, Precondition precond) throws Exception {
		try {
			List nodes = doc.selectNodes(xpath);
			for (Object node : nodes) {
				Node element = (Node) node;
				if (precond == null || 
						(	precond.matches(element.getText()) &&
							precond.matches(element))
						) {
						op.apply(element);
				}
			}
		}catch (PatternSyntaxException e) {
			throw new Exception(e.getMessage()); 
		}catch (InvalidXPathException e) {
			throw new Exception(e.getMessage());
		}catch (InvalidXPathTypeException e) {
			throw new Exception(e.getMessage());
		}
	}
}
