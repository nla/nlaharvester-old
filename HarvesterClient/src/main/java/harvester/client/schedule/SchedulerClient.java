package harvester.client.schedule;

import harvester.client.util.*;
import harvester.data.Contributor;
import harvester.data.Profile;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.xml.xpath.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * This class handles almost all tasks related to scheduling. This includes converting from the view's
 * representation of a schedule to a format that can be directly passed to the Scheduler WS, as well
 * as making the WS requests to get and put a webservice for both the complex case of a production 
 * schedule and the simpler case of a test or harvestnow schedule.
 * It is interacted with by the Ineract method for the harvest now stuff, and the EditScheduleController,
 * ModifyScheduleController for production harvests, and editmanualharvestcontroller for test harvests. 
 * 
 */
@SuppressWarnings("static-access")
public class SchedulerClient {

    protected final Log logger = LogFactory.getLog(SchedulerClient.class);
    
    /**   the web service urls needed to connect to the scheduler and the harvest processor
    These are filled in by the IOC container. */
	private String wsurl;
    /**   the web service urls needed to connect to the scheduler and the harvest processor
    These are filled in by the IOC container. */	
	private String harvesterurl;
	
	/**
	 * 	we have to do alot of date manipulation. see link below for explanation of format for these
	<a href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>
	 */
	private static String userdateformat = "dd.MM.yyyy HH:mm";
	private static String userdateformatshort = "dd.MM.yyyy";	
	public static String oaiformatshort = "yyyy-MM-dd";
	public static String oaiformat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	/** Possible value for the from field */
	public static final Integer FROM_LAST_SUCC = 1;
	/** Possible value for the from field */
	public static final Integer FROM_EARLIEST = 2;
	/** Possible value for the from field */
	public static final Integer FROM_GIVEN_DATE = 3;
	
	public String getHarvesterurl() {
		return harvesterurl;
	}

	public void setHarvesterurl(String harvesterurl) {
		this.harvesterurl = harvesterurl;
	}

	public String getWsurl() {
		return wsurl;
	}

	public void setWsurl(String wsurl) {
		this.wsurl = wsurl;
	}

	public SchedulerClient()
	{
	}
	
	/**
	 * Gets a single schedule
	 * @param jobid jobid of the schedule to get
	 * @return the schedule object
	 * @throws Exception
	 */
	public Schedule getSchedule(String jobid) throws Exception
	{
		LinkedList<String> jobids = new LinkedList<String>();
		jobids.add(jobid);
		List<Schedule> ss = getSchedule(jobids);
		if(ss != null && ss.size() != 0)
			return ss.get(0);	//first
		else
			return null;
	}
	
	/**
	 * gets a list of schedules
	 * @param jobids a list of schedule ids
	 * @return a list of schedules
	 * @throws Exception
	 */
	public List<Schedule> getSchedule(List<String> jobids) throws Exception
	{
		List<Schedule> ss = new LinkedList<Schedule>();
		
		//do a web request, readding the result into an xml document
		
		logger.info("connecting to scheduler ws");
		StringBuilder url = new StringBuilder();
		//url.append("?scheduleraction=getschedule");
			
		for(String jobid : jobids)
		{
			url.append("&jobid=");
			url.append(URLEncoder.encode(jobid, "UTF-8"));
		}
		
		url = url.replace(0, 1, "?");		
		URL requesturl = new URL(wsurl + url.toString());
		logger.info(wsurl + url.toString());
		
		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
		conn.setRequestMethod("GET");
		
		conn.connect();
		if(conn.getResponseCode() != HttpURLConnection.HTTP_OK)
		{
			logger.error("schedule not found");
			return null;
		}
		
        DOMParser parser = new DOMParser();
        try
        {
		    InputStream in = conn.getInputStream();
		    InputStreamReader isr = new InputStreamReader(in, "UTF-8");
		    InputSource source = new InputSource(isr);
		    parser.parse(source);
	        in.close();
        } catch (Exception e)
        {
        	logger.error("Error connecting to WS");
			return null;
        }
        conn.disconnect();
        Document doc = parser.getDocument();
        
        logger.info("got xml document from ws");
		
        //read all the values we need into schedule object
        //isn't xpath great??
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        
        double num = (Double)xpath.evaluate("count(/schedules/schedule)", doc, XPathConstants.NUMBER);
        logger.info(num + " schedules returned");
        int inum = (int)num;
        for(int i = 1; i <= inum; i++)
        {
	        Schedule s = new Schedule();
	        String rt = "/schedules/schedule[" + i + "]/";
	        s.setId(xpath.evaluate(rt + "@id", doc));
	        s.setEnabled(xpath.evaluate(rt + "@enabled", doc));
	        s.setLast(xpath.evaluate(rt + "lastjob/text()", doc));
	        s.setNext(xpath.evaluate(rt + "nextjob/text()", doc));
	        s.setLastUTC(xpath.evaluate(rt + "lastjobUTC/text()", doc));
	        s.setNextUTC(xpath.evaluate(rt + "nextjobUTC/text()", doc));	        
	        s.setDescription(xpath.evaluate( rt + "description/text()", doc));
	        s.setFrom(xpath.evaluate(rt + "jobdetails/detail[@key=\"from\"]/text()", doc));
	        s.setDelete(xpath.evaluate(rt + "jobdetails/detail[@key=\"delete\"]/text()", doc));
	        //not sure if these are needed anymore
	        if(s.getFrom() != null && s.getFrom() == "")
	        	s.setFrom(null);
	        //if(s.getFrom() != null && s.getFrom().equals("FIRST"))	//These lines make no sense
	        //	s.setFrom("");
	        logger.info("from is:" + s.getFrom());
	        
	       //multiple crons
	        s.setCrons(new LinkedList<String>());
	        NodeList crons = (NodeList)xpath.evaluate(rt + "cron", doc, XPathConstants.NODESET);
	        logger.info("CRONS:" + crons.getLength());
	        for(int j =0; j < crons.getLength(); j++)
	        {
	        	logger.info("CRON EXPRESSION:" + crons.item(j).getFirstChild().getNodeValue());
	        	s.getCrons().add(crons.item(j).getFirstChild().getNodeValue());
	        }
	        
	        ss.add(s);
        }
        
		return ss;
	}

	/**
	 * Converts from a {@link Schedule} object to a {@link ScheduleView} object
	 * @param schedule schedule to convert
	 * @param c contributor data object
	 * @return schedule view built
	 */
	public ScheduleView buildView(Schedule schedule, Contributor c) {
		
		ScheduleView sv = new ScheduleView();
		//easy ones first
		if(schedule.getEnabled().equalsIgnoreCase("true"))
			sv.setEnabled(true);
		
		// begin,from
		// set them to default values
		sv.setBegin(sv.BEGIN_DEFAULT);
		sv.setFrom(sv.FROM_DEFAULT);
		sv.setShortgran(c.getGranularity() == 0);
		
		logger.info("from=" + schedule.getFrom());
		
		//delete stuff
		if("notharvested".equals(schedule.getDelete()))
			sv.setDelete(true);
		
		//from date
		if(schedule.getFrom() != null)
		{
				if(c.getIsfinishedfirstharvest() != null && c.getIsfinishedfirstharvest() == Contributor.NOT_FIRST_HARVEST)
				{
					//this is not the first run of this schedule, so don't show the passed from field
					logger.info("this is not the first harvest, so setting next harvest stuff to defaults");
					sv.setFrom(sv.FROM_DEFAULT);
					sv.setFromdate(null);
					
				} else
				{
					if(schedule.getFrom().equals("LASTRUN"))
					 {
						 logger.info("setting from to from last production harvest");
						 sv.setFrom(sv.FROM_DEFAULT);
						 sv.setFromdate(null);
		 
				 	 } else if(schedule.getFrom().equals("FIRST"))
					 {
						 logger.info("setting from to from first record");
						 sv.setFrom(sv.FROM_ALL);
						 sv.setFromdate(null);
					 } else	
					 {		
						 logger.info("setting from to the passed in from");
						 try
						 {
							Format oaiformatter = new SimpleDateFormat(oaiformat);
							Format userformatter = new SimpleDateFormat(c.getGranularity() == 0 ? userdateformatshort : userdateformat);
							DateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm");
							utc.setTimeZone(TimeZone.getTimeZone("UTC"));
							Date fromdate = (Date) oaiformatter.parseObject(schedule.getFrom());
							sv.setFromdate(userformatter.format(fromdate));
							sv.setFromdateUTC(utc.format(fromdate));
							sv.setFrom(sv.FROM_SET);
						 } catch (Exception e)
						 {
							 logger.error("PROBLEM: could not parse passed from field, using from last");
							 sv.setFrom(sv.FROM_DEFAULT);
							 sv.setFromdate(null);							 
						 }
					 }					
				}
		}
		
		//description
		sv.setDescription(schedule.getDescription());
		
		//job id
		if(schedule.getId() != null)
			sv.setJobid(Integer.valueOf(schedule.getId()));
		
		//cron string encode almost everything about the execution time
		//we use the first cron expression given to get the day, month info
		////String firstcron = schedule.getCrons().getFirst();
		
		//each part of a cron expression is space delimitered    
		//to get the day,week month information we need one of the crons that
		// is not the beginat cron, so we find the first cron that ends in *
		String standardcron = null;	//there should always be a cron ending in *
		for(String cron : schedule.getCrons())
			if(cron.endsWith("*")) {
				standardcron = cron;
				break;
			}
		//if(standardcron == null)
		//	return null;
		
		String[] parts = standardcron.split(" ");
		
		//4th is day of month
		String dayofmonth = parts[3];
		logger.info("dayofmonth: " + dayofmonth);
		sv.setDates(new LinkedList<Integer>());
		//the list of days is comma delimitered
		if(!(dayofmonth.equals("*") || dayofmonth.equals("?") ))
		{
			for(String day : dayofmonth.split(","))
				sv.getDates().add(Integer.valueOf(day));
		}
		else
		{
			sv.getDates().add(1);
		}
		
		//5th is month
		String months = parts[4];
		logger.info("month: " + months);
		sv.setMonths(new HashMap<String, Boolean>());
		for(String month : months.split(","))
			sv.getMonths().put(month, true);
		
		//6th is day of the week
		String days = parts[5];
		logger.info("days: " + days);
		sv.setDays(new HashMap<String, Boolean>());
		for(String day : days.split(","))
			sv.getDays().put(day, true);
		
		//if a day has been selected then this is a weekly schedule, otherwise monthly
		if(sv.getDays().get("?") != null)
			sv.setWeekly(false);
		else
			sv.setWeekly(true);
		logger.info("WEEKLY:" + sv.isWeekly());
		
		//we assume that the given cron expresssions differ only in the min/hour fields
		sv.setTimes(new LinkedList<ViewTime>());
		for(String cron : schedule.getCrons())
		{
			String[] cronparts = cron.split(" ");
			
			//if this cron expression has a year field other then *, then we know that it is a begin field
			if(!cron.endsWith("*"))
			{
				sv.setBegin(sv.BEGIN_SET);
				Calendar bc = Calendar.getInstance();
				bc.set(Integer.valueOf(cronparts[6]), Integer.valueOf(cronparts[4])-1, Integer.valueOf(cronparts[3]),
						Integer.valueOf(cronparts[2]), Integer.valueOf(cronparts[1]) );
				Format beginformat = new SimpleDateFormat(userdateformat);
				sv.setBegindate(beginformat.format(bc.getTime()));
				continue;
			}
			
			//ignore the first part, since we don't work with seconds
			
			//2nd part is minutes, 3rd is hours
			ViewTime vt = new ViewTime();
			vt.setMinute(Integer.valueOf(cronparts[1]));
			vt.setHour(Integer.valueOf(cronparts[2]));
			sv.getTimes().add(vt);
			
		}
		
		//NOTE: the view requires at  least one time
		// this code should always provide that, or crash :-)
		
		return sv;
	}

	/**
	 * Sends the changes in the passed schedule view to the actual scheduler WS.
	 * The beginjobat field is handled by sending it both as a cron field and as a job detail.
	 * @param sv Schedule view containing changes
	 * @param c contributor data object
	 * @throws Exception
	 */
	public void makeScheduleModifications(ScheduleView sv, Contributor c) throws Exception {
		
		//granularity stuff
		sv.setShortgran(c.getGranularity() == 0 ? true : false);
		
		// first get the schedule	
		Schedule s = convertViewToSchedule(sv);

		//make sure this contributor is flagged as having a schedule 
		c.setIsfinishedfirstharvest(Contributor.THIS_IS_FIRST_HARVEST);
		c.setIsscheduled(1);	
		
		sendPutSchedule(s, c, null);
	}
	
	/**
	 * 
	 * @param s
	 * @param c
	 * @param type Should be null for production schedules
	 * @throws Exception
	 */
	public void sendPutSchedule(Schedule s, Contributor c, Boolean type) throws Exception {
		//we can now build up the request url with all the info from the schedule object
		StringBuilder doc = new StringBuilder();

		doc.append("<schedule id=\"");
		doc.append(EscapeHTML.forXML(s.getId()));
		doc.append("\" enabled=\"");
		doc.append(s.getEnabled());
		doc.append("\">\n");
		if(s.getDescription() != null)
			doc.append("<description>" + EscapeHTML.forXML(s.getDescription()) + "</description>\n");
		
		if(s.getCrons() != null)
			for(String cron : s.getCrons())
				doc.append("<cron>" + EscapeHTML.forXML(cron) + "</cron>\n");
		
		logger.info("s beginat=" + s.getBeginjobat());
		if(s.getBeginjobat() != null)
			doc.append("<beginjobat>" + EscapeHTML.forXML(s.getBeginjobat()) + "</beginjobat>\n");
		
		doc.append("<jobdetails>");
		
		doc.append("<detail key=\"url\">" + EscapeHTML.forXML(harvesterurl) + "</detail>");
		doc.append("<detail key=\"contributorid\">" + 
				EscapeHTML.forXML(String.valueOf(c.getContributorid())) + "</detail>");
		doc.append("<detail key=\"action\">start</detail>");
		doc.append("<detail key=\"task\">0</detail>");
		
		if(type == null) {	//this case is the norm for a production schedule
			doc.append("<detail key=\"type\">" + EscapeHTML.forXML(String.valueOf(c.getType())) + "</detail>");
			doc.append("<detail key=\"profileid\">" + 
					EscapeHTML.forXML( c.getProduction() == null ? "" : String.valueOf(c.getProduction().getProfileid()) )
					+ "</detail>");
		} else {	//TODO: I'm not exactly sure if this case is needed, or even if it works. It should be looked at!

			doc.append("<detail key=\"type\">" + (type ? "0" : "1") + "</detail>");
			
			if(type && c.getTest() != null) {
				doc.append("<detail key=\"profileid\">" + EscapeHTML.forXML(String.valueOf(c.getTest().getProfileid()) ) + "</detail>");
			}
			if( !type && c.getProduction() != null) {
				doc.append("<detail key=\"profileid\">" + EscapeHTML.forXML(String.valueOf(c.getProduction().getProfileid()) ) + "</detail>");
			}
		}
		if(s.getFrom() != null)
			doc.append("<detail key=\"from\">" + EscapeHTML.forXML(s.getFrom()) + "</detail>");
		if(s.getUntil() != null)
			doc.append("<detail key=\"until\">" + EscapeHTML.forXML(s.getUntil()) + "</detail>");		
		if(s.getUntil50() != null)
			doc.append("<detail key=\"until50\">" + EscapeHTML.forXML(s.getUntil50()) + "</detail>");	
		//TODO: these might be able to be merged into one
		if("notharvested".equals(s.getDelete()))
			doc.append("<detail key=\"delete\">notharvested</detail>");
		if("delete".equals(s.getDelete()))
			doc.append("<detail key=\"delete\">delete</detail>");		
		
		doc.append("</jobdetails></schedule>");		
		
		sendPutRequest(wsurl + URLEncoder.encode(s.getId(), "UTF-8"), doc.toString());
	}
	
	/**
	 * Converts a Schedule view to a schedule
	 * @param sv Schedule View
	 * @return schedule after conversion
	 * @throws Exception
	 */
	public Schedule convertViewToSchedule(ScheduleView sv) throws Exception
	{
		Schedule s = new Schedule();
		
		//easy ones first
		s.setId(String.valueOf(sv.getJobid()));
		
		if(sv.isEnabled())
			s.setEnabled("true");
		else
			s.setEnabled("false");
		
		s.setShortgran(sv.isShortgran());
		
		//now for the cron field
		s.setCrons(new LinkedList<String>());
		String Seconds = "0";
		String DayOfMonth = null;
		String Month = null;
		String DayOfWeek = null;
		
		if(sv.isDelete())
			s.setDelete("notharvested");
		
		if(sv.isWeekly())
		{
			//we set dayomonth and month as per instructions : http://www.opensymphony.com/quartz/wikidocs/CronTriggers%20Tutorial.html
			DayOfMonth = "?";
			Month = "*";				
			DayOfWeek = setToCommaSeperatedString(sv.getDays().keySet());	
		}
		else
		{
			//MONTHLY
			DayOfWeek = "?";
			Month = setToCommaSeperatedString(sv.getMonths().keySet());
			logger.info("num days: " + sv.getDates().size());
			DayOfMonth = listToCommaSeperatedString(sv.getDates());
		}
		
		//we also need to build up a list of times for the human readable description
		StringBuilder times = new StringBuilder();
		
		//each time results in a different cron expression, so there are multiple cron expressions created
		for(ViewTime vt : sv.getTimes())
		{
			String Minutes = String.valueOf(vt.getMinute());
			//we need to add the leading zero for the minutes display if minutes is in 0-9
			if(vt.getMinute() < 10)
				Minutes = "0" + Minutes;
			
			String Hours = String.valueOf(vt.getHour());
			
			String cron = Seconds + " " + Minutes + " " + Hours + " " + DayOfMonth + " " + Month + " " + DayOfWeek + " *";
			logger.info("cron is " + cron);
			s.getCrons().add(cron);
			
			times.append(Hours + ":" + Minutes + ",");
		}

		/////for the description ////////////////////////////
		DayOfMonth = listOfDatesPrettyPrint(sv.getDates());
		if(times.length() == 0)
			logger.info("this should not happen");
		else
			times.deleteCharAt(times.length()-1);
		
		if(DayOfWeek == null || DayOfWeek.equals("") || DayOfWeek.equals("*"))
			DayOfWeek = "every day";
		else
			DayOfWeek = " every " + DayOfWeek;
		//if(DayOfMonth == null || DayOfMonth.equals("") || DayOfMonth.equals("*"))
		//	DayOfMonth = "every month";
		if(Month == null || Month.equals("") || Month.equals("*"))
			Month = " every month";
		else
			Month = " of " + Month;

		if(sv.isWeekly())
			s.setDescription("At " + times + DayOfWeek);
		else
			s.setDescription("At " + times + " on the " + DayOfMonth + Month);
		/////////////////////////////////////////////////////////
		
		//we need a date formatter to work with user specified dates for the radio option stuff
		Format inputformat = new SimpleDateFormat(sv.isShortgran() ? userdateformatshort : userdateformat );
		
		//if they have used the "at scheduled time" field we need to add another cron expression to the cron list
		// with this time in it.
		if( sv.getBegin() == sv.BEGIN_SET)
		{
			logger.info("begin set");
			Date begindate = null;
			try
			{
				Format longformat = new SimpleDateFormat(userdateformat );
				begindate = (Date)longformat.parseObject(sv.getBegindate());
			} catch (Exception e)
			{
				throw new Exception("Unabled to comprehend entered from date");
			}
			if(begindate == null)
			{
				throw new Exception("Examining the from date entered returned empty");
			}
			
			Format cronformatter = new SimpleDateFormat("0 mm HH dd MM ? yyyy");
			String cron = cronformatter.format(begindate);
			
			if(begindate.after(new Date()) ) {
				s.getCrons().add(cron);
			}
			
			s.setBeginjobat(cron);
		}
		if(sv.getFrom() == sv.FROM_SET)
		{
			logger.info("converting the from field, input=" + sv.getFromdate());
			//we need to convert the date format into a Date type, then into the 
			//OAI format
			Date fromdate = (Date) inputformat.parseObject(sv.getFromdate());
			Format oai = new SimpleDateFormat(oaiformat);
			s.setFrom(oai.format(fromdate));
			logger.info("from output=" + s.getFrom());
		} else if(sv.getFrom() == sv.FROM_ALL)
		{
			s.setFrom("FIRST");
		} else if(sv.getFrom() == sv.FROM_DEFAULT)
		{
			s.setFrom("LASTRUN");
		}
		
		return s;
	}
	
	/**
	 * Converts a Set of Strings to a comma delimitered representation in a string
	 * @param set the set
	 * @return the string representation.
	 */
	private String setToCommaSeperatedString(Set<String> set)
	{
		StringBuilder st = new StringBuilder();
		
		for(String v : set)
		{
			st.append(v);
			st.append(",");
		}
		if(st.length() == 0)
			st.append("*");
		else
			st.deleteCharAt(st.length()-1);
			
		return st.toString();
	}
	
	/**
	 * Converts a list of integers to a comma delimitered list in a string
	 * @param list
	 * @return string representation
	 */
	private String listToCommaSeperatedString(LinkedList<Integer> list)
	{
		StringBuilder st = new StringBuilder();
		
		for(Integer v : list)
		{
			st.append(v.toString());
			st.append(",");
			logger.info("day:: " + v.toString());
		}
		if(st.length() == 0)
			st.append("*");
		else
			st.deleteCharAt(st.length()-1);
			
		return st.toString();
	}
	
	/**
	 * Pretty formats a list of dates by adding suffixes e.g. 1,2,3 -> "1st, 2nd, 3rd"
	 * @param list input list of dates
	 * @return formatted string
	 */
	private String listOfDatesPrettyPrint(LinkedList<Integer> list)
	{
		StringBuilder st = new StringBuilder();
		
		for(Integer v : list)
		{
			String suffix = null;
			if(v == 1)
				suffix = "st";
			else if (v == 2)
				suffix = "nd";
			else if (v == 3)
				suffix = "rd";
			else
				suffix = "th";
			st.append(v.toString() + suffix);
			st.append(", ");
			logger.info("day:: " + v.toString());
		}
		if(st.length() == 0)
			st.append("*");
		else {
			st.deleteCharAt(st.length()-1);
			st.deleteCharAt(st.length()-1);
		}
			
		return st.toString();
	}
	

	/**
	 * Fetches the Schedule View for this contributor, which is the production scheduled one.
	 * @param c contributor data object
	 * @return Schedule View
	 * @throws Exception
	 */
	public ScheduleView getScheduleView(Contributor c) throws Exception{
		
		Schedule schedule = null;
		try
		{
			schedule = getSchedule(String.valueOf(c.getContributorid()));
		} catch (Exception e)
		{
			schedule = null;
		}
		
		ScheduleView sv = null;
		if(schedule != null)
			sv = buildView(schedule, c);
		else
			sv = ScheduleView.getDefaultScheduleView();
		
		return sv;
	}
	
	/**
	 * Runs a manual test harvest with the parameters specified
	 * @param harvestdate String representation of the data in userdateformat
	 * @param fromEarliest (only checked if form is null) what should the from field be(filled with from enum). If false and from is null, do from last successful
	 * @param from If harvesting from a given date, pass the date in here
	 * @param until until date in user date format(if specified)
	 * @param until50 should this harvest be stopped at 50 records?
	 * @param delete contains a string specifiying how records should be deleted before/after the harvest
	 * @param c contributor data access object
	 * @throws Exception
	 */
	public void runManualHarvest(String harvestdate, int fromEarliest, String from, String until, boolean until50, String delete, Contributor c) throws Exception
	{		
		runManualHarvest(harvestdate, fromEarliest, from, until, c, true, until50, delete);
	}
	
	/**
	 * Runs a manual test harvest with the parameters specified(date formats depend on contributors granularity where approprete)
	 * @param harvestdate String representation of the data in userdateformat(
	 * @param fromtype (only checked if from is null)fromEarliest what should the from field be(filled with from enum). If false and from is null, do from last successful
	 * @param from If harvesting from a given date, pass the date in here
	 * @param until until date in userdateformat(if specified)
	 * @param until50 should this harvest be stopped at 50 records?
	 * @param delete contains a string specifying how records should be deleted before/after the harvest
	 * @param c contributor data access object
	 * @param test should this be a test harvest?
	 * @throws Exception
	 */
	public void runManualHarvest(String harvestdate, int fromtype, String from, String until, Contributor c, boolean test, boolean until50, String delete) throws Exception
	{
		
		logger.info("long run manual harvest method called");
		
		//firstly we need to parse the three dates we have in there correct formats
		logger.info("from: " + from + " until:" + until + " harvestdate:" + harvestdate  + " test=" + test);
		
		//all three are recieved in the user format
		//from and until need to be in OAI format
		Format userformatter = new SimpleDateFormat(c.getGranularity() == 1 ? userdateformat : userdateformatshort);
		Format oai = new SimpleDateFormat(oaiformat);
		
		String fromoai = null;
		if(fromtype == FROM_GIVEN_DATE)
		{
			Date fromdate = (Date)userformatter.parseObject(from);
			fromoai = oai.format(fromdate);
		} else if( fromtype == FROM_EARLIEST)
		{
			fromoai = "FIRST";
		} else if( fromtype == FROM_LAST_SUCC)
		{
			fromoai = "LASTRUN";
		}
		
		String untiloai = null;
		if(until != null)
		{
			Date untildate = (Date)userformatter.parseObject(until);
			untiloai = oai.format(untildate);
		}
		
		//harvest date needs to be a cron expression
		String cron = null;
		if(harvestdate != null)
		{
			Format userformatterlong = new SimpleDateFormat(userdateformat);
			Date crondate = (Date)userformatterlong.parseObject(harvestdate);
			if(crondate.after(new Date()))
			{
				Format cronformatter = new SimpleDateFormat("0 mm HH dd MM ? yyyy");
				cron = cronformatter.format(crondate);
			} else cron = "";
		} else
			cron = "";

		
		//we only need to fill out the parameters we use
		Schedule s = new Schedule();
		s.setId(String.valueOf(c.getContributorid()));
		if(test)
			s.setId(s.getId() + "TEST");	// if it doesn't have the test bit then it would show up under production harvests
		s.setEnabled("true");
		Boolean type = test;
		s.setFrom(fromoai);
		LinkedList<String> crons = new LinkedList<String>();
		crons.add(cron);
		s.setCrons(crons);
		s.setUntil(untiloai);
		if(until50) s.setUntil50("true");
		s.setDelete("delete");
		sendPutSchedule(s, c, type);
//		
//		
//		//following code is much like the other send function
//		
//		//we can now build up the request url with all the info from the schedule object
//		StringBuilder url = new StringBuilder();
//		url.append(wsurl + "?scheduleraction=putschedule&jobid=");
//		//id is just the contributoid plus the current time + from time
//		url.append(URLEncoder.encode(String.valueOf(c.getContributorid()) + "TEST","UTF-8"));
//		url.append("&cron=");
//		url.append(URLEncoder.encode(cron, "UTF-8"));
//		url.append("&url=");
//		url.append(URLEncoder.encode(harvesterurl, "UTF-8"));
//		url.append("&contributorid=");
//		url.append(c.getContributorid());
//		if(test)
//			url.append("&type=0");	
//		else
//			url.append("&type=1");
//		
//		if(test && c.getTest() != null)
//		{
//			url.append("&profileid=");
//			url.append(c.getTest().getProfileid());
//		}
//		if( !test && c.getProduction() != null)
//		{
//			url.append("&profileid=");
//			url.append(c.getProduction().getProfileid());
//		}
//
//		url.append("&action=start&task=0");
//		if(fromoai != null)
//		{
//			url.append("&from=");
//			url.append(URLEncoder.encode(fromoai, "UTF-8"));
//		}
//		if(untiloai != null)
//		{
//			url.append("&until=");
//			url.append(URLEncoder.encode(untiloai, "UTF-8"));
//		}
//		if(until50)
//		{			
//			url.append("&until50=true");
//		}
//		url.append("&delete=");
//		url.append(delete);
//		
//		logger.info("url :" + url.toString());
//		
//		logger.info("connecting to scheduler ws");
//		URL requesturl = new URL(url.toString());
//		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
//		conn.setRequestMethod("GET");
//		
//		conn.connect();
//		logger.info("Response code:" + conn.getResponseCode());
//		conn.disconnect();
		
	}
	
	
	/**
	 * 	This method is invoked by the ListHarvest view and the ViewharvestSchedule view in order to 
	run an already scheduled harvest imediately.
	if they want to harvest now and no schedule is set up, a runmanual harvest command is issued instead
	 * @param contributorid
	 */
	public void runHarvestNow(int contributorid) throws Exception
	{
		//scheduler ws has an action doschedule which when not passed a cron field will
		//execute the schedule now.
		//String url = wsurl + "?scheduleraction=doschedule&jobid=" + String.valueOf(contributorid);			
		//responselessRequest(url);
		
		logger.info("run harvest now called with just a contributor id");
		
		String url = wsurl + contributorid;
		//application/x-www-form-urlencoded
		logger.info("url :" + url);
		
		logger.info("connecting to scheduler ws");
		URL requesturl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
		conn.connect();
		logger.info("Response code:" + conn.getResponseCode());
		conn.disconnect();
	}

	public void deleteProductionSchedules(String scheduleid) throws Exception
	{
		
		logger.info("delete production schedules method called");
		
		String url = wsurl + scheduleid;
		logger.info("url :" + url);
		
		logger.info("connecting to scheduler ws");
		URL requesturl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
		conn.setRequestMethod("DELETE");
		
		conn.connect();
		logger.info("Response code:" + conn.getResponseCode());
		conn.disconnect();

	}
	
//	/** send a request that doesn't expect a response back  */
//	public void responselessRequest(String url) throws Exception {
//		logger.info("url :" + url.toString());
//		
//		logger.info("connecting to scheduler ws");
//		URL requesturl = new URL(url);
//		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
//		conn.setRequestMethod("GET");
//		
//		conn.connect();
//		logger.info("Response code:" + conn.getResponseCode());
//		conn.disconnect();
//	}
	
	/**
	 * Run a single record harvest imediately
	 * @param recordid
	 * @param c contributor data object
	 * @throws Exception
	 */
	public void runManualHarvest(String recordid, Contributor c) throws Exception {
		
		logger.info("runManual harvest for single record harvesting called");
		
		StringBuilder doc = new StringBuilder();

		doc.append("<schedule id=\"");
		doc.append(EscapeHTML.forXML(String.valueOf(c.getContributorid())));
		doc.append("\" enabled=\"true\">\n");
		doc.append("<cron></cron>\n");

		doc.append("<jobdetails>");
		
		doc.append("<detail key=\"url\">" + EscapeHTML.forXML(harvesterurl) + "</detail>");
		doc.append("<detail key=\"contributorid\">" + 
				EscapeHTML.forXML(String.valueOf(c.getContributorid())) + "</detail>");
		doc.append("<detail key=\"action\">start</detail>");
		doc.append("<detail key=\"task\">0</detail>");
		doc.append("<detail key=\"profileid\">" + EscapeHTML.forXML(String.valueOf(c.getTest().getProfileid()) ) + "</detail>");
		doc.append("<detail key=\"action\">start</detail>");
		doc.append("<detail key=\"task\">0</detail>");
		doc.append("<detail key=\"type\">" + Profile.TEST_PROFILE + "</detail>");
		doc.append("<detail key=\"singlerecord\">" + EscapeHTML.forXML(recordid) + "</detail>");
		
		doc.append("</jobdetails></schedule>");		
		
		sendPutRequest(wsurl + URLEncoder.encode(String.valueOf(c.getContributorid()), "UTF-8"), doc.toString());
	}
	
	/**
	 * get the current time in the userdateformat short
	 * @return current formatted time
	 */
	public static String getNowShort()
	{
		DateFormat f = new SimpleDateFormat(userdateformatshort);
		return f.format(new Date());		
	}
	/**
	 * get the current time in the userdateformat long
	 * @return current formatted time
	 */
	public static String getNow()
	{
		DateFormat f = new SimpleDateFormat(userdateformat);
		return f.format(new Date());		
	}
	
	private void sendPutRequest(String url, String xml) throws Exception{
		
		logger.debug("url : " + url);
		logger.debug("xml : " + xml);
		
		logger.info("connecting to scheduler ws");
		URL requesturl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
		conn.setRequestMethod("PUT");
		conn.setRequestProperty("Content-type", "text/xml");
		conn.setDoOutput(true);
		conn.connect();
		OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream()); 
		w.write(xml);
		w.flush();
		w.close();
		logger.info("Response code:" + conn.getResponseCode());

		conn.disconnect();
	}
	
	
}
