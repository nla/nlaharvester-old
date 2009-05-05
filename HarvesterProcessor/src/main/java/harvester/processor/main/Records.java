package harvester.processor.main;

import java.util.LinkedList;

/**
 * data container for passing record data between steps. keeps track of records, deleted records and a few statistics
 */
public class Records {

	/** regular records, probably in dom4j format */
	private LinkedList<Object> records = new LinkedList<Object>();
	/** records marked for deletion */
	private LinkedList<Object> deleted_records = new LinkedList<Object>();
	/** this flag can be set by a step if the pipeline should not process any
	 *  more batches(currently only checked/set for harvest step */
	private boolean continue_harvesting = true;
	/** records recieved at the start of this batch */
	private int total_records;
	private int deletionsPerformed;
	private int recordsAdded;
	/** keeps track of how many records the repository reports to have in total. Repositories rarely tell us though */
	private Integer recordsinsource;


	public int getRecordsAdded() {
		return recordsAdded;
	}

	public void setRecordsAdded(int recordsAdded) {
		this.recordsAdded = recordsAdded;
	}

	public int getDeletionsPerformed() {
		return deletionsPerformed;
	}

	public void setDeletionsPerformed(int deletionsPerformed) {
		this.deletionsPerformed = deletionsPerformed;
	}

	public Integer getRecordsinsource() {
		return recordsinsource;
	}

	public void setRecordsinsource(Integer recordsinsource) {
		this.recordsinsource = recordsinsource;
	}

	public boolean isContinue_harvesting() {
		return continue_harvesting;
	}

	public void setContinue_harvesting(boolean continue_harvesting) {
		this.continue_harvesting = continue_harvesting;
	}

	public Records(Records records) {
		setTotalRecords(records.getTotalRecords());
		continue_harvesting = records.isContinue_harvesting();
		recordsinsource = records.getRecordsinsource();
		deleted_records = records.getDeletedRecords();
		setDeletionsPerformed(records.getDeletionsPerformed());
		setRecordsAdded(records.getRecordsAdded());
	}	

	public Records() {
		total_records = 0;
		deletionsPerformed = 0;
	}

	public LinkedList<Object> getRecords() {
		return records;
	}
	public void setRecords(LinkedList<Object> records) {
		this.records = records;
	}
	public LinkedList<Object> getDeletedRecords() {
		return deleted_records;
	}
	public void setDeletedRecords ( LinkedList<Object> deleted_records) {
		this.deleted_records = deleted_records;
	}
	public int getTotalRecords() {
		return total_records;
	}
	public void setTotalRecords(int total_records) {
		this.total_records = total_records;
	}
	/**
	 * current records in this container
	 * @return records.size() + deleted_records.size() 
	 */
	public int getCurrentrecords() {
		return records.size();
		//return records.size() + deleted_records.size();
	}
	
	public void addRecord(Object record) {
		records.add(record);
	}
	public void addDeletedRecord(Object record) {
		deleted_records.add(record);
	}
	
}
