package harvester.processor.util;

import java.util.regex.*;
import org.dom4j.*;
import java.util.*;

/** Used to parse xpaths so they can be created using the picker in the add step.
 *
*/
public class XPathParser {
	String prefix;
	String attribute;
	String element;

	boolean is_attribute;

	LinkedList<Attribute> attributes;

	public class Attribute {
		String name;
		String value;
		public Attribute(String name, String value) {
			this.name = name;
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	}

	public String getElement() {
		return element;
	}
	public String getPrefix() {
		return prefix;
	}
	public String getAttribute() {
		return attribute;
	}
	public boolean isAttribute() {
		return is_attribute;
	}
	public LinkedList<Attribute> getAttributes() {
		return attributes;
	}

	/** The xpath is called on construction which throws an Exception on error
	 */
	public XPathParser(String xpath) throws Exception {
		attributes = new LinkedList<Attribute>();
		Pattern pattern;
		Matcher matcher;

		/* used to split xpath by the last slash, eg, /doc/test[@name='foo'] => /doc, test[@name='foo'] */
		pattern = Pattern.compile("(.*)/(.*)");

		matcher = pattern.matcher(xpath);

		if (!matcher.find()) {
			throw new Exception("Could not find last forward slash in xpath."); 
		}
		prefix = matcher.group(1);
		String postfix = matcher.group(2);

		if (postfix.matches("^@.*")) { 			/* is the postfix an attribute? */
			is_attribute = true;							/* this is easy, but non-validating. */
			attribute = postfix.substring(1); /* strip the @ */
		}else {
			is_attribute = false;
			element = postfix;
			/* this matches attributes */
			pattern = Pattern.compile("\\[\\s*@(\\S+?)\\s*=\\s*'(.*?)'\\s*\\]\\s*");
			matcher = pattern.matcher(postfix);
			while (matcher.find()) {
				attributes.add(new Attribute(matcher.group(1), matcher.group(2)));
			}
			/* this matches the element name */
			pattern = Pattern.compile("^\\s*([a-zA-Z]+)");
			matcher = pattern.matcher(postfix);
			if (!matcher.find()) {
				throw new Exception("Could not find valid element name");
			}
			element = matcher.group(1);
		}
	}
}
