package harvester.client.schedule;

import java.util.*;
/**
 * This class represents a "schedule" in a format very close to what is returned by the schduler WS.
 * It is converted to and from a scheduler view object as needed, by methods in SchedulerClient.
 * Almost everything is stored as a string since this is the format straight from the xml document.
 * It is also used by the view schedule page since that page doesn't need the processed information
 */
public class Schedule {

	private String enabled;
	private String description;
	private String last;
	private String next;
	private String nextUTC;
	private String lastUTC;
	private String lastSuccessful;
	public String getLastSuccessful() {
		return lastSuccessful;
	}


	public void setLastSuccessful(String lastSuccessful) {
		this.lastSuccessful = lastSuccessful;
	}


	private String lastSuccessfulUTC;

	private String id;
	private String delete;
	
	private String beginJobAt;
	
	private String from;
	private String until;
	private String until50;
	private String oldfrom;
	private boolean shortGran = true;
	
	//when the "at scheduled time" field is used,
	//a cron with a year field is in this list,
	//otherwise it is one cron per time given(literal meaning of time)
	private LinkedList<String> crons;
		
	public String getUntil() {
		return until;
	}


	public void setUntil(String until) {
		this.until = until;
	}


	public String getUntil50() {
		return until50;
	}


	public void setUntil50(String until50) {
		this.until50 = until50;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}
	public String getEnabled() {
		return enabled;
	}


	public void setEnabled(String enabled) {
		this.enabled = enabled;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getLast() {
		return last;
	}


	public void setLast(String last) {
		this.last = last;
	}


	public String getNext() {
		return next;
	}


	public void setNext(String next) {
		this.next = next;
	}

	public Schedule()
	{
		
	}
	
	public LinkedList<String> getCrons() {
		return crons;
	}

	public void setCrons(LinkedList<String> crons) {
		this.crons = crons;
	}


	public String getFrom() {
		return from;
	}


	public void setFrom(String from) {
		this.from = from;
	}


	public String getOldfrom() {
		return oldfrom;
	}


	public void setOldfrom(String oldfrom) {
		this.oldfrom = oldfrom;
	}


	public boolean isShortgran() {
		return shortGran;
	}


	public void setShortgran(boolean shortgran) {
		this.shortGran = shortgran;
	}


	public String getBeginjobat() {
		return beginJobAt;
	}


	public void setBeginjobat(String beginjobat) {
		this.beginJobAt = beginjobat;
	}


	public String getDelete() {
		return delete;
	}


	public void setDelete(String delete) {
		this.delete = delete;
	}


	public String getNextUTC() {
		return nextUTC;
	}


	public void setNextUTC(String nextUTC) {
		this.nextUTC = nextUTC;
	}


	public String getLastUTC() {
		return lastUTC;
	}


	public void setLastUTC(String lastUTC) {
		this.lastUTC = lastUTC;
	}
	
	public String getLastSuccessfulUTC() {
		return lastSuccessfulUTC;
	}


	public void setLastSuccessfulUTC(String lastSuccessfulUTC) {
		this.lastSuccessfulUTC = lastSuccessfulUTC;
	}
		
}
