package converter.transform;

import java.util.*;
import java.net.*;
import java.text.*;
import java.util.regex.*;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.*;
import org.apache.velocity.runtime.RuntimeConstants;
import org.dom4j.*;
import org.glowacki.CalendarParser;

import converter.*;

/**
 * The Transform class is really just a pretty abstraction on the velocity templating library with a few handy text/date processing features.
*/
public class Transform {
	
	private static Logger logger = Logger.getLogger(Transform.class);
	
	Document doc;
	boolean matched;
	VelocityEngine ve;
	
	public Transform() {
		matched = false;
	}
	public Transform(Document doc) {
		this.doc = doc;
		matched = false;
	}
	
	public boolean getMatched() {
		return matched;
	}

	/**
	 * This is where all the text processing stuff is.
	*/
	public class Convert {

		/**
		 * The Name class is used for normalizing names. Eg,
		 *
		 * Tor Lattimore
		 * Lattimore, Tor
		 * Tor A. Lattimore
		 * Tor Alexander Lattimore
		 * Lattimore, Tor Alexander
		*/
		public class Name {
			String first_name;
			String last_name;
			String initial;

			boolean is_valid;

			public String getFirstName() {return first_name;}
			public String getLastName() {return last_name;}
			public String getInitial() {return initial;}

			public boolean isValid() {return is_valid;}
			Name(String text) {
				is_valid = false;

				Pattern pattern;
				Matcher matcher;
				
				pattern = Pattern.compile("^\\s*([^\\s\\,\\.]+)\\s+([^\\s\\,\\.]+)\\s*$");
				matcher = pattern.matcher(text);
				if (matcher.find()) {
					is_valid = true;
					first_name = matcher.group(1);
					last_name = matcher.group(2);
					initial = "";
					return;
				}

				pattern = Pattern.compile("^\\s*([^\\s\\,\\.]+)\\s+([^\\s])\\.\\s+([^\\s\\,\\.]+)\\s*$");
				matcher = pattern.matcher(text);
				if (matcher.find()) {
					is_valid = true;
					first_name = matcher.group(1);
					last_name = matcher.group(3);
					initial = matcher.group(1);
					return;
				}

				pattern = Pattern.compile("^\\s*([^\\s\\,\\.]+)\\s*\\,\\s*([^\\s\\,\\.]+)\\s*$");
				matcher = pattern.matcher(text);
				if (matcher.find()) {
					is_valid = true;
					first_name = matcher.group(2);
					last_name = matcher.group(1);
					initial = "";
					return;
				}
			}
		}

		/**
		 * Gets a name from a piece of text.
		*/
		public Name getName(String text) {
			return new Name(text);
		}

		/**
		 * Parses a date in a given format. Returns an empty string if an exception is thrown
		 */
		public String parseDate(String input, String format) {
			Date date;
			try {
				date = CalendarParser.parse(input).getTime();	
			}catch (Exception e) {
				return "";
			}	
			DateFormat formatter = new SimpleDateFormat(format);
			return formatter.format(date);
		}

		/** 
		 * Get the current date in a particular format
		 */
		public String date(String format) {
			DateFormat formatter = new SimpleDateFormat(format);
			return formatter.format(new Date());
		}
	
		/**
		 * Returns the text of the first matching xpath
		 */
		public String getFirst(String xpath) {
			String text = "";
			List nodes = doc.selectNodes(xpath);
			for (Object node : nodes) {
				return ((Node)node).getText();
			}
			return text;
		}

		/**
		 * Returns the text of the last matching xpath
		 */
		public String getLast(String xpath) {
			String text = "";
			List nodes = doc.selectNodes(xpath);
			for (Object node : nodes) {
				if (((Node)node).getText() != null) {
					text = ((Node)node).getText();
				}
			}
			return text;
		}
		/**
		 * Returns the text of the element with the longest text and matching the xpath.
		 */
		public String getLongest(String xpath) {
			String text = "";
			List nodes = doc.selectNodes(xpath);
			for (Object obj : nodes) {
				Node node = (Node)obj;
				if (node.getText() != null && node.getText().length() > text.length()) {
					text = node.getText();
				}
			}
			return text;
		}
	}
	/**
	 * Convert the input, regex, template and node to new text 
	 */
	public String convert(String input, String regex, String output, Node element) throws Exception {
		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

		Matcher matcher = pattern.matcher(input);
		
		String new_expr = output;
		String new_text = "";
		String text = input;
	    int count = 0;
		while (matcher.find() && count < 100) {
			count+=1;
			matched = true;
			StringWriter w;
		 	w = new StringWriter();
			VelocityContext context = new VelocityContext();
			context.put("C", new Convert());
			context.put("D", doc);
			if (element != null) {
				context.put("E", element);
			}
			for (int i = 1;i <= matcher.groupCount();i++) {
				context.put("g" + Integer.toString(i), matcher.group(i));
			}
			try {
				Velocity.evaluate(context, w, "mystring", output);
			}catch (ParseErrorException e) {
				throw new Exception("Parse error in template<br /> " + e.getMessage());
			}
			new_expr = w.toString();
			new_text+=text.substring(0,matcher.start());
			new_text+=new_expr;
			text = text.substring(matcher.end());
			if (text.length() == 0) {
				break;
			}
			matcher = pattern.matcher(text);
		}
		new_text+=text;
		return new_text;
	}
}











