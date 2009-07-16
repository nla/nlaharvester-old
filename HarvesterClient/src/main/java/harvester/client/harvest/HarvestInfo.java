package harvester.client.harvest;

import harvester.data.Harvest;
import harvester.data.Profile;

import java.util.Date;

/**
 * Holds the view representation of the status of a harvest for use by the harvest worktray.
 * Information such as last harvest time, status, records counts are stored in this.
 */
public class HarvestInfo {
	
	private int contributorid;
	private Integer harvestid;
	private String contributorname;
	private String time;
	private String status;
	private Integer recordsrejected;
	private Integer rejectedpercentage;
	private Integer goodpercentage;
	private Integer recordcount;
	/** OAI, WEB_CRAWL or OTHER */
	private int type;
	/** SUCCESSFUL, RUNNING or FAILED */
	private int statuscode;
	private Date timedate;
	private int sortingby;
	
	public static final int OAI = Profile.OAI;
	public static final int Z3950 = Profile.Z3950;
	
	public static final int SUCCESSFUL = Harvest.SUCCESSFUL;
	public static final int RUNNING = Harvest.RUNNING;
	public static final int FAILED = Harvest.FAILED;
	
	public static final int SCHEDULED_ENABLED = 0;
	public static final int SCHEDULED_DISABLED = 1;
	
	public static int getSCHEDULED_ENABLED() {
		return SCHEDULED_ENABLED;
	}
	public static int getSCHEDULED_DISABLED() {
		return SCHEDULED_DISABLED;
	}
	
	public Date getTimedate() {
		return timedate;
	}
	public void setTimedate(Date timedate) {
		this.timedate = timedate;
	}
	public int getContributorid() {
		return contributorid;
	}
	public void setContributorid(int contributorid) {
		this.contributorid = contributorid;
	}
	public String getContributorname() {
		return contributorname;
	}
	public void setContributorname(String contributorname) {
		this.contributorname = contributorname;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Integer getRecordsrejected() {
		return recordsrejected;
	}
	public void setRecordsrejected(Integer recordsrejected) {
		this.recordsrejected = recordsrejected;
	}
	public Integer getRejectedpercentage() {
		return rejectedpercentage;
	}
	public void setRejectedpercentage(Integer rejectedpercentage) {
		this.rejectedpercentage = rejectedpercentage;
	}
	public Integer getRecordcount() {
		return recordcount;
	}
	public void setRecordcount(Integer recordcount) {
		this.recordcount = recordcount;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public static int getOAI() {
		return OAI;
	}
	public static int getZ3950() {
		return Z3950;
	}

	public Integer getHarvestid() {
		return harvestid;
	}
	public void setHarvestid(Integer harvestid) {
		this.harvestid = harvestid;
	}
	public int getStatuscode() {
		return statuscode;
	}
	public void setStatuscode(int statuscode) {
		this.statuscode = statuscode;
	}
	public static int getSUCCESSFUL() {
		return SUCCESSFUL;
	}
	public static int getRUNNING() {
		return RUNNING;
	}
	public static int getFAILED() {
		return FAILED;
	}
	public int getSortingby() {
		return sortingby;
	}
	public void setSortingby(int sortingby) {
		this.sortingby = sortingby;
	}
	public Integer getGoodpercentage() {
		return goodpercentage;
	}
	public void setGoodpercentage(Integer goodpercentage) {
		this.goodpercentage = goodpercentage;
	}
}
