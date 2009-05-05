package harvester.client.data.dao.interfaces;

import java.util.Date;
import java.util.List;
import harvester.data.Report;

public interface ReportDAO {

	public List<Report> getReports(int collectionid);
	
	public Report getReport(int reportid);
	
	public List<Object[]> getHarvestDatesReport(int collectionid, Date startdate, Date enddate);
	public List<Object[]> getContributorsReport(int collectionid, Date startdate, Date enddate);
	public List<Object[]> getRejectedRecordsReport(int collectionid, Date startdate, Date enddate);
	public List<Object[]> getRecordsByCollectionReport(int collectionid, Date startdate, Date enddate);
	public List<Object[]> getHarvestErrorsReport(int collectionid, Date startdate, Date enddate);
	
	public void addReport(Report r);
	public String getReportData(int reportid);
	public void deleteReport(int reportid);
	
}
