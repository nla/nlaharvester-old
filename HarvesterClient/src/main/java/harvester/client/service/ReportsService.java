package harvester.client.service;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import harvester.client.data.dao.DAOFactory;
import harvester.client.web.ReportsController;
import harvester.data.Collection;
import harvester.data.Contributor;
import harvester.data.Report;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Ostermiller.util.ExcelCSVPrinter;

@Service
public class ReportsService {

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
	
	public Collection getCollection(int collectionid) {
		return daofactory.getCollectionDAO().getCollection(collectionid);
	}
	
	public List<Report> getReports(int collectionid) {
		return daofactory.getReportDAO().getReports(collectionid);
	}
	
	public Map<Integer, String> getTypes() {
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		map.put(Report.REJECTED_RECORDS, "Rejected Records");
		map.put(Report.HARVEST_DATES, "Harvest Dates");
		//map.put(Report.SCHEDULING, "Scheduling");
		map.put(Report.CONTRIBUTORS, "Contributors by Collection");
		map.put(Report.RECORDS_BY_COLLECTION, "Records by Collection");
		map.put(Report.HARVEST_ERRORS, "Harvest Errors by Collection");
		return map;
	}
	
	public String getReportData(int reportid) {
		return daofactory.getReportDAO().getReportData(reportid);
	}
	public void deleteReport(int reportid) {
		daofactory.getReportDAO().deleteReport(reportid);
	}
	public Report getReport(int reportid) {
		return daofactory.getReportDAO().getReport(reportid);
	}
	
	public void generateHarvestDatesReport(int collectionid, Date startdate, Date enddate) {

		String csv = toCSV(daofactory.getReportDAO().getHarvestDatesReport(collectionid, startdate, enddate));
		generateReport(collectionid, csv, Report.HARVEST_DATES, startdate, enddate);
	}
	public void generateRejectedRecordsReport(int collectionid, Date startdate, Date enddate) {

		String csv = toCSV(daofactory.getReportDAO().getRejectedRecordsReport(collectionid, startdate, enddate));
		generateReport(collectionid, csv, Report.REJECTED_RECORDS, startdate, enddate);
	}
	public void generateRecordsByCollectionReport(int collectionid, Date startdate, Date enddate) {

		String csv = toCSV(daofactory.getReportDAO().getRecordsByCollectionReport(collectionid, startdate, enddate));
		generateReport(collectionid, csv, Report.RECORDS_BY_COLLECTION, startdate, enddate);
	}
	public void generateHarvestErrorsReport(int collectionid, Date startdate, Date enddate) {

		String csv = toCSV(daofactory.getReportDAO().getHarvestErrorsReport(collectionid, startdate, enddate));
		generateReport(collectionid, csv, Report.HARVEST_ERRORS, startdate, enddate);
	}
	
	public void generateContributorsReport(int collectionid, Date startdate, Date enddate) {
		
		List<Object[]> report = daofactory.getReportDAO().getContributorsReport(collectionid, startdate, enddate);
		
		//name, date added, type, format, location
		
		//change the htypes to the textual version
		boolean header = true;
		for(Object[] row : report) {
			if(header) {
				header = false;
				continue;
			}
			if(Integer.valueOf(row[2].toString()).equals(Contributor.OAI))
				row[2] = "OAI";
			else
				row[2] = "OTHER";	//TODO when web crawling is added change this
		}
		
		generateReport(collectionid, toCSV(report), Report.CONTRIBUTORS, startdate, enddate);
	}
	
	
	public void generateReport(int collectionid, String csv, int reporttype, Date startdate, Date enddate) {
		try {
			
			SimpleDateFormat df = new SimpleDateFormat(ReportsController.reportFileNameDate);
			
			//add the date range line to the start of every report
			csv = "Report date range: " + df.format(startdate) + " - " + df.format(enddate) + "\n" + csv;
			
			Report r = new Report();
			r.setData(Hibernate.createBlob(csv.getBytes("UTF-8")));
			r.setTimestamp(new Date());
			r.setType(reporttype);
			r.setStartdate(startdate);
			r.setEnddate(enddate);
			
			Collection collection = new Collection();
			collection.setCollectionid(collectionid);
			r.setCollection(collection);
			
			daofactory.getReportDAO().addReport(r);
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	
	private String toCSV(List<Object[]> report) {
		
		StringWriter w = new StringWriter();
		ExcelCSVPrinter csv = new ExcelCSVPrinter(w);
		
		for(Object[] row : report) {
			String[] stringrow = new String[row.length];
		    for(int i = 0; i < row.length; i++) {
		    	if(row[i] == null)
		    		stringrow[i] = "";
		    	else
		    		stringrow[i] = row[i].toString();
		    }
		    csv.println(stringrow);
		}	
		
		return w.toString();
	}
}
