package harvester.client.util;


import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *  http://www.javapractices.com/topic/TopicAction.do?Id=96
 *  used in many places in the code for escaping strings.
 *  I should probably being using the spring library for this,
 *  but I didn't know it existed when I needed this.
 * 
 * 
 * @author adefazio
 *
 */
public class EscapeHTML {

   public static String forHTML(String aText){
	 
	 if(aText == null)
		 return null;
	 
     final StringBuilder result = new StringBuilder();
     final StringCharacterIterator iterator = new StringCharacterIterator(aText);
     char character =  iterator.current();
     while (character != CharacterIterator.DONE ){
       if (character == '<') {
         result.append("&lt;");
       }
       else if (character == '>') {
         result.append("&gt;");
       }
       else if (character == '&') {
         result.append("&amp;");
      }
       else if (character == '\"') {
         result.append("&quot;");
       }
       else if (character == '\'') {
         result.append("&#039;");
       }
       else if (character == '(') {
         result.append("&#040;");
       }
       else if (character == ')') {
         result.append("&#041;");
       }
       else if (character == '#') {
         result.append("&#035;");
       }
       else if (character == '%') {
         result.append("&#037;");
       }
       else if (character == ';') {
         result.append("&#059;");
       }
       else if (character == '+') {
         result.append("&#043;");
       }
       else if (character == '-') {
         result.append("&#045;");
       }
       else {
         //the char is not a special one
         //add it to the result as is
         result.append(character);
       }
       character = iterator.next();
     }
     return result.toString();
  }
  
  
  /***
   * Synonym for <tt>URLEncoder.encode(String, "UTF-8")</tt>.
   *
   * <P>Used to ensure that HTTP query strings are in proper form, by escaping
   * special characters such as spaces.
   *
   * <P>It is important to note that if a query string appears in an <tt>HREF</tt>
   * attribute, then there are two issues - ensuring the query string is valid HTTP
   * (it is URL-encoded), and ensuring it is valid HTML (ensuring the 
   * ampersand is escaped).
   */
   public static String forURL(String aURLFragment){
     String result = null;
     try {
       result = URLEncoder.encode(aURLFragment, "UTF-8");
     }
     catch (UnsupportedEncodingException ex){
       throw new RuntimeException("UTF-8 not supported", ex);
     }
     return result;
   }

  /***
  * Escape characters for text appearing as XML data, between tags.
  * 
  * <P>The following characters are replaced with corresponding character entities : 
  * <table border='1' cellpadding='3' cellspacing='0'>
  * <tr><th> Character </th><th> Encoding </th></tr>
  * <tr><td> < </td><td> &lt; </td></tr>
  * <tr><td> > </td><td> &gt; </td></tr>
  * <tr><td> & </td><td> &amp; </td></tr>
  * <tr><td> " </td><td> &quot;</td></tr>
  * <tr><td> ' </td><td> &#039;</td></tr>
  * </table>
  * 
  * <P>Note that JSTL's {@code <c:out>} escapes the exact same set of 
  * characters as this method. <span class='highlight'>That is, {@code <c:out>}
  *  is good for escaping to produce valid XML, but not for producing safe HTML.</span>
  */
  public static String forXML(String aText){
    final StringBuilder result = new StringBuilder();
    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
    char character =  iterator.current();
    while (character != CharacterIterator.DONE ){
      if (character == '<') {
        result.append("&lt;");
      }
      else if (character == '>') {
        result.append("&gt;");
      }
      else if (character == '\"') {
        result.append("&quot;");
      }
      else if (character == '\'') {
        result.append("&#039;");
      }
      else if (character == '&') {
         result.append("&amp;");
      }
      else {
        //the char is not a special one
        //add it to the result as is
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }

  /***
  * Return <tt>aText</tt> with all <tt>'<'</tt> and <tt>'>'</tt> characters
  * replaced by their escaped equivalents.
  */
  public static String toDisableTags(String aText){
    final StringBuilder result = new StringBuilder();
    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
    char character =  iterator.current();
    while (character != CharacterIterator.DONE ){
      if (character == '<') {
        result.append("&lt;");
      }
      else if (character == '>') {
        result.append("&gt;");
      }
      else {
        //the char is not a special one
        //add it to the result as is
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }
  

  /***
  * Replace characters having special meaning in regular expressions
  * with their escaped equivalents, preceded by a '\' character.
  *
  * <P>The escaped characters include :
  *<ul>
  *<li>.
  *<li>\
  *<li>?, * , and +
  *<li>&
  *<li>:
  *<li>{ and }
  *<li>[ and ]
  *<li>( and )
  *<li>^ and $
  *</ul>
  *
  */
  public static String forRegex(String aRegexFragment){
    final StringBuilder result = new StringBuilder();

    final StringCharacterIterator iterator = new StringCharacterIterator(aRegexFragment);
    char character =  iterator.current();
    while (character != CharacterIterator.DONE ){
      /**
      * All literals need to have backslashes doubled.
      */
      if (character == '.') {
        result.append("\\.");
      }
      else if (character == '\\') {
        result.append("\\\\");
      }
      else if (character == '?') {
        result.append("\\?");
      }
      else if (character == '*') {
        result.append("\\*");
      }
      else if (character == '+') {
        result.append("\\+");
      }
      else if (character == '&') {
        result.append("\\&");
      }
      else if (character == ':') {
        result.append("\\:");
      }
      else if (character == '{') {
        result.append("\\{");
      }
      else if (character == '}') {
        result.append("\\}");
      }
      else if (character == '[') {
        result.append("\\[");
      }
      else if (character == ']') {
        result.append("\\]");
      }
      else if (character == '(') {
        result.append("\\(");
      }
      else if (character == ')') {
        result.append("\\)");
      }
      else if (character == '^') {
        result.append("\\^");
      }
      else if (character == '$') {
        result.append("\\$");
      }
      else {
        //the char is not a special one
        //add it to the result as is
        result.append(character);
      }
      character = iterator.next();
    }
    return result.toString();
  }
  
  /***
  * Disable all <tt><SCRIPT></tt> tags in <tt>aText</tt>.
  * 
  * <P>Insensitive to case.
  */  
  public static String forScriptTagsOnly(String aText){
    String result = null;
    Matcher matcher = SCRIPT.matcher(aText);
    result = matcher.replaceAll("&lt;SCRIPT>");
    matcher = SCRIPT_END.matcher(result);
    result = matcher.replaceAll("&lt;/SCRIPT>");
    return result;
  }
  
  // PRIVATE //
  
  private static final Pattern SCRIPT = Pattern.compile(
    "<SCRIPT>", Pattern.CASE_INSENSITIVE
   );
  private static final Pattern SCRIPT_END = Pattern.compile(
    "</SCRIPT>", Pattern.CASE_INSENSITIVE
  );
}
