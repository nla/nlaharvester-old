package harvester.client.util;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A few useful utility methods. Note that this class can be used directly in a velocity view.
 *
 */
public class WebUtil {

    protected static final Log logger = LogFactory.getLog(WebUtil.class);
	
    public DateFormat formatter;
    public DateFormat shortformatter;
    
    //currently only used in EditScheduling, but should be used in more places
	public static int getIntFieldOrError(String fieldname, HttpServletRequest request) throws Exception
	{
		try {
			 return Integer.valueOf((String) request.getParameter(fieldname));
		} catch (Exception e) {
			//TODO error page
			logger.error("error with passed " + fieldname);
			for(StackTraceElement ste : e.getStackTrace())
				logger.error(ste.toString());
			throw new Exception("error with passed " + fieldname);
		}
		
	}
	
	
	// This useful method is from the internet. It just converts from an
	// inputstream, which has no inherent type of data or encoding, to
	// a string. It assumes UTF-8 encoding, which is normally reasonable.
	// For better performance, pass in a buffered inputstream.
	public static String slurp (InputStream in) throws IOException {
	    StringBuffer out = new StringBuffer();
	    byte[] b = new byte[4096];
	    for (int n; (n = in.read(b)) != -1;) {
	        out.append(new String(b, 0, n, "UTF-8"));
	    }
	    return out.toString();
	}

	/////////////////////////////////////////////////////////////////////////
	//These two functions are used by a velocity page to do date conversion 
	//in the page. That is why the object must be initilizable. If I used
	//freemarker instead this wouldn't be needed
	
	public WebUtil()
	{
		formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		shortformatter = new SimpleDateFormat("dd.MM.yyyy");
	}
	
	
	public String userformat(Date date)
	{
		return formatter.format(date);
	}
	
	public String currentTZShortFormat(Date date) {
		return shortformatter.format(date);
	}
	
	public static String formatFuzzyDate(Date date)
	{
		Calendar cal = new GregorianCalendar(TimeZone.getDefault());
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date Today = cal.getTime();
		cal.add(Calendar.DATE, -7);
		Date OneWeekAgo = cal.getTime();
		
		cal.setTime(date);
		
		if(date.after(Today))
		{
			DateFormat timeformat = new SimpleDateFormat("'Today at' h:mm a");
			return timeformat.format(date);
			
		} else if( date.after(OneWeekAgo))
		{
			DateFormat timeformat = new SimpleDateFormat("'Last' EEEE 'at ' h:mm a");			
			return timeformat.format(date);
		} else
		{
			//Note that this code completely disregards timezones, daylight savings etc.
			// which is why I only report a rough date
			
			//this one has to be done fairly manually
			long interval = Today.getTime() - date.getTime();
			long week = 60 * 60 * 24 * 7 * 1000; //ms in week
			
			long weeksago = interval / week;
					
			return weeksago + ( weeksago == 1 ? " week ago" : " weeks ago");
		}
	}
	

	
}
