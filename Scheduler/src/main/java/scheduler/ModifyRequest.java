package scheduler;

import java.util.*;

import org.dom4j.*;

/**
 * Encapsulates the information from a request and provides xml serialization and deserialization of the request.
 * @author adefazio
 *
 */
public class ModifyRequest {
	String jobid;
	String jobenabled;
	List<String> crons;
	String description;
	String beginjobat;
	Map<String, String> jobdetails;
	
	/*
	 * Extract the fields out of the given xml, into this type
	 */
	@SuppressWarnings("unchecked")
	public ModifyRequest(String xml) throws DocumentException {
		
		Document doc = DocumentHelper.parseText(xml);

		//value of gives us an empty string if the field doesn't exist for some reason
		jobid = doc.valueOf("schedule/@id");
		jobenabled = doc.valueOf("schedule/@enabled");
		description = doc.valueOf("schedule/description");
		beginjobat = doc.valueOf("schedule/beginjobat");
		if("".equals(jobid)) jobid = null;
		if("".equals(description)) description = null;
		if("".equals(jobenabled)) jobenabled = null;
		if("".equals(beginjobat)) beginjobat = null;
		
		crons = new LinkedList<String>();
		List<Element> cronnodes = doc.selectNodes("schedule/cron");
		for(Element cron : cronnodes)
			crons.add(cron.getText());
		
		jobdetails = new HashMap<String, String>();
		List<Element> detailnodes = doc.selectNodes("schedule/jobdetails/detail");
		for(Element detail : detailnodes)
			jobdetails.put(detail.attributeValue("key"), detail.getText());
		
	}
	
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("jobid=" + jobid + "\n");
		sb.append("jobenabled=" + jobenabled + "\n");
		sb.append("description=" + description + "\n");
		sb.append("beginjobat=" + beginjobat + "\n");
		sb.append("crons=" + crons + "\n");
		sb.append("jobdetails=" + jobdetails);
		
		return sb.toString();
		
	}
	
	
	
	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

	public String getJobenabled() {
		return jobenabled;
	}

	public void setJobenabled(String jobenabled) {
		this.jobenabled = jobenabled;
	}

	public List<String> getCrons() {
		return crons;
	}

	public void setCrons(List<String> crons) {
		this.crons = crons;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBeginjobat() {
		return beginjobat;
	}

	public void setBeginjobat(String beginjobat) {
		this.beginjobat = beginjobat;
	}

	public Map<String, String> getJobdetails() {
		return jobdetails;
	}

	public void setJobdetails(Map<String, String> jobdetails) {
		this.jobdetails = jobdetails;
	}
}
