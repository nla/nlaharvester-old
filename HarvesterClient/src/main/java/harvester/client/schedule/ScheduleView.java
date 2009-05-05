package harvester.client.schedule;

import harvester.client.util.*;

import java.util.LinkedList;
import java.util.Map;

/**
 * This class represents a schedule in a format very close to what is displayed to the user.
 * In fact, the view works directly from an object of this class when rendering the view.
 */
public class ScheduleView {
	
	private String description;
	private int jobid;
	
	private boolean enabled;
	private boolean weekly;
	private boolean shortgran = true;
	private boolean delete = false;
	
	private Map<String, Boolean> days;
	
	//must not be empty for view
	private LinkedList<Integer> dates;
	
	private Map<String,Boolean> months;
	
	//must not be empty for view
	private LinkedList<ViewTime> times;
	
	private int begin;	//set with values from static fields below
	private String begindate;
	
	private int from; //set with values from static fields below
	private String fromdate;
	private String fromdateUTC;
	
	public static int BEGIN_DEFAULT = 1;
	public static int BEGIN_SET = 2;
	public static int FROM_DEFAULT = 1;
	public static int FROM_ALL = 2;
	public static int FROM_SET = 3;

	//This is exactly the format that the specs seemed to want for the default view.
	public static ScheduleView getDefaultScheduleView()
	{
		ScheduleView sv = new ScheduleView();
		LinkedList<Integer> dates = new LinkedList<Integer>();
		dates.add(1);
		sv.setDates(dates);
		LinkedList<ViewTime> times = new LinkedList<ViewTime>();
		times.add(new ViewTime(17, 0));
		sv.setTimes(times);
		
		sv.setBegin(1);
		sv.setFrom(1);
		sv.setEnabled(true);
		sv.setWeekly(false);
		
		return sv;
	}

	
	

	////////////////////////////////////////////////////////////////
	///GETTERS and SETTERS
	
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isWeekly() {
		return weekly;
	}

	public void setWeekly(boolean weekly) {
		this.weekly = weekly;
	}

	public Map<String, Boolean> getDays() {
		return days;
	}

	public void setDays(Map<String, Boolean> days) {
		this.days = days;
	}

	public LinkedList<Integer> getDates() {
		return dates;
	}

	public void setDates(LinkedList<Integer> dates) {
		this.dates = dates;
	}

	public Map<String, Boolean> getMonths() {
		return months;
	}

	public void setMonths(Map<String, Boolean> months) {
		this.months = months;
	}

	public LinkedList<ViewTime> getTimes() {
		return times;
	}

	public void setTimes(LinkedList<ViewTime> times) {
		this.times = times;
	}

	public int getBegin() {
		return begin;
	}

	public void setBegin(int begin) {
		this.begin = begin;
	}

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}




	public String getDescription() {
		return description;
	}




	public void setDescription(String description) {
		this.description = description;
	}




	public int getJobid() {
		return jobid;
	}




	public void setJobid(int jobid) {
		this.jobid = jobid;
	}




	public String getBegindate() {
		return begindate;
	}




	public void setBegindate(String begindate) {
		this.begindate = begindate;
	}




	public String getFromdate() {
		return fromdate;
	}




	public void setFromdate(String fromdate) {
		this.fromdate = fromdate;
	}




	public boolean isShortgran() {
		return shortgran;
	}




	public void setShortgran(boolean shortgran) {
		this.shortgran = shortgran;
	}




	public boolean isDelete() {
		return delete;
	}




	public void setDelete(boolean delete) {
		this.delete = delete;
	}




	public String getFromdateUTC() {
		return fromdateUTC;
	}




	public void setFromdateUTC(String fromdateUTC) {
		this.fromdateUTC = fromdateUTC;
	}
}
