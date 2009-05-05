package harvester.client.web;

import harvester.client.data.dao.DAOFactory;
import harvester.client.service.ReportsService;
import harvester.client.util.WebUtil;
import harvester.data.Report;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@SuppressWarnings("unchecked")
public class ReportsController {

    protected final Log logger = LogFactory.getLog(getClass());
	private ReportsService reportsService;
	
	@Autowired
	public void setReportsService(ReportsService reportsService) {
		this.reportsService = reportsService;
	}

	public static String reportfilterdateformat = "dd.MM.yyyy"; //"dd/MM/yyyy";  //"yyyy-MM-dd";
	public static String reportFileNameDate = "yyyy-MM-dd";
	

	@RequestMapping("/ListReports.htm")
	public String listReports(@RequestParam("collectionid") int collectionid, Map model) { 

		model.put("collection", reportsService.getCollection(collectionid));
		model.put("reports", reportsService.getReports(collectionid));
		model.put("dateformat", new WebUtil());		
		model.put("typemap", reportsService.getTypes());

		//startdatedefault
		SimpleDateFormat df = new SimpleDateFormat(reportfilterdateformat);
		Calendar cal = GregorianCalendar.getInstance();
		cal.add(GregorianCalendar.MONTH, -1);	//last month

		int startDayOfMonth = cal.getActualMinimum(GregorianCalendar.DAY_OF_MONTH);
		cal.set(GregorianCalendar.DAY_OF_MONTH, startDayOfMonth);
		model.put("startdatedefault", df.format(cal.getTime()));
		
		int endDayOfMonth = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		cal.set(GregorianCalendar.DAY_OF_MONTH, endDayOfMonth);
		model.put("enddatedefault", df.format(cal.getTime()));
		
        return "ListReports";
	}
	
	@RequestMapping("/GenerateReport.htm")
	public String generateReport(@RequestParam("collectionid") int collectionid,
								 @RequestParam("reporttype") int reporttype,
								 @RequestParam("startdate") String startdateStr,
								 @RequestParam(value="enddate", required=false) String enddateStr,
								 @RequestParam(value="groupby", required=false) String groupby ){
		
		SimpleDateFormat filterformat = new SimpleDateFormat(reportfilterdateformat);
		Date startdate = null;
		Date enddate = null;
		try { startdate = filterformat.parse(startdateStr); } catch (Exception e) {}
		try { enddate = filterformat.parse(enddateStr); } catch (Exception e) {}
		
		if(reporttype == Report.HARVEST_DATES) {
			reportsService.generateHarvestDatesReport(collectionid, startdate, enddate);
		}
		if(reporttype == Report.CONTRIBUTORS) {
			reportsService.generateContributorsReport(collectionid, startdate, enddate);
		}
		if(reporttype == Report.REJECTED_RECORDS) {
			reportsService.generateRejectedRecordsReport(collectionid, startdate, enddate);
		}
		if(reporttype == Report.RECORDS_BY_COLLECTION) {
			reportsService.generateRecordsByCollectionReport(collectionid, startdate, enddate);
		}
		if(reporttype == Report.HARVEST_ERRORS) {
			reportsService.generateHarvestErrorsReport(collectionid, startdate, enddate);
		}
		
		return "redirect:ListReports.htm?collectionid=" + collectionid;
	}
	
	@RequestMapping("/DeleteReport.htm")
	public String deleteReport(@RequestParam("collectionid") int collectionid,
							   @RequestParam("reportid") int reportid) {
		
		reportsService.deleteReport(reportid);
		
		return "redirect:ListReports.htm?collectionid=" + collectionid;
	}
	
	@RequestMapping("/ViewCSVReport.htm")
	public String viewCSVReport(@RequestParam("reportid") int reportid,
							    HttpServletResponse response) throws IOException {
		
		Report r = reportsService.getReport(reportid);
		SimpleDateFormat dateformat = new SimpleDateFormat(reportFileNameDate);
		
		String filename = dateformat.format(r.getTimestamp())
						  + "-" + reportsService.getTypes().get(r.getType()).replace(' ', '-') + ".csv";
		
		logger.info("Response buffer size: " + response.getBufferSize());
		response.setContentType("text/csv;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.addHeader("Content-Disposition", "attachment; filename=" + filename);
		ServletOutputStream outputStream = response.getOutputStream();

		outputStream.print(reportsService.getReportData(reportid));
		
		outputStream.flush();
		outputStream.close();
		return null;
	}
}
