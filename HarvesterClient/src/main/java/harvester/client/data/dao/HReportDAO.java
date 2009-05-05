package harvester.client.data.dao;

import java.util.Date;
import java.util.List;

import harvester.client.data.dao.interfaces.ReportDAO;
import harvester.client.util.WebUtil;
import harvester.data.Contributor;
import harvester.data.HarvestLog;
import harvester.data.ParameterInformation;
import harvester.data.Report;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unchecked")
public class HReportDAO implements ReportDAO{

	protected final Log logger = LogFactory.getLog(HContributorDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	@Transactional(readOnly=true)
	public List<Report> getReports(int collectionid) {
		List<Report> reports = (List<Report>) sf.getCurrentSession()
		.createSQLQuery("select * from report r where r.collectionid = " + String.valueOf(collectionid) + " order by r.timestamp")
		.addEntity(Report.class).list();
		
		for(Report report : reports) {
			Hibernate.initialize(report.getContributor());
		}
		
		return reports;
	}
	
	@Transactional
	public void addReport(Report r) {
		sf.getCurrentSession().save(r);
	}
	
	@Transactional
	public void deleteReport(int reportid) {
		Report r = (Report)sf.getCurrentSession().load(Report.class, reportid);
		sf.getCurrentSession().delete(r);
	}
	
	@Transactional
	public String getReportData(int reportid) {
		try {
			Report r = (Report)sf.getCurrentSession().load(Report.class, reportid);
			return WebUtil.slurp(r.getData().getBinaryStream());
		} catch (Exception e) {
			logger.error("error loading report data, " + e.getMessage(), e);
			return null;
		}
	}

	@Transactional
	public Report getReport(int reportid) {
		Report r = (Report)sf.getCurrentSession().load(Report.class, reportid);
		Hibernate.initialize(r);
		return r;
	}
	
	@Transactional(readOnly=true)
	public List<Object[]> getHarvestDatesReport(int collectionid, Date startdate, Date enddate) {
		
		SQLQuery reportQuery = sf.getCurrentSession().createSQLQuery(
				"select name, status, starttime from contributor c, harvest h "
			  + "where c.contributorid = h.contributorid AND collectionid=:collectionid "
			  + "AND starttime > :startdate AND starttime < :enddate " 
			  + "order by name");
		reportQuery.setInteger("collectionid", collectionid);
		reportQuery.setDate("startdate", startdate);
		reportQuery.setDate("enddate", enddate);
		
		Object[] header = {"Contributor", "Harvest Status", "Harvest Date"};
		List<Object[]> report = reportQuery.list();
		report.add(0, header);
		return report;
	}
	
	@Transactional(readOnly=true)
	public List<Object[]> getContributorsReport(int collectionid, Date startdate, Date enddate) {
		
		//NOTE this is sort of fragile since it will break if another parameterinfo is added with either of these names
		//but that is very unlikely
		ParameterInformation format = (ParameterInformation) sf.getCurrentSession()		
			.createQuery("from ParameterInformation as pi where pi.parametername = 'Metadata Prefix'").uniqueResult();
		ParameterInformation location = (ParameterInformation) sf.getCurrentSession()
			.createQuery("from ParameterInformation as pi where pi.parametername = 'Base URL'").uniqueResult();
		
		
		SQLQuery reportQuery = sf.getCurrentSession().createSQLQuery(
			 "select name, dateadded, htype, "
			+"(select value from profilestepparameter psp where psp.psid = p.psid AND piid= :formatid) as format, "
			+"(select value from profilestepparameter psp where psp.psid = p.psid AND piid= :locationid) as location " 
			+"from contributor c left join profilestep p on p.psid = c.psid "
			+"where collectionid=:collectionid AND dateadded > :startdate AND dateadded < :enddate "
			+"order by name ASC");
		reportQuery.setInteger("collectionid", collectionid);
		reportQuery.setDate("startdate", startdate);
		reportQuery.setDate("enddate", enddate);
		reportQuery.setInteger("formatid", format.getPiid());
		reportQuery.setInteger("locationid", location.getPiid());
		
		Object[] header = {"Contributor", "Date Added", "Harvest Type", "Format", "Location"};
		List<Object[]> report = reportQuery.list();
		report.add(0, header);
		return report;
		
	}
	
	@Transactional(readOnly=true)
	public List<Object[]> getRejectedRecordsReport(int collectionid, Date startdate, Date enddate) {
		SQLQuery reportQuery = sf.getCurrentSession().createSQLQuery(
			"select c.name, "
		  + "(select name from step where stepid = hl.stepid) as step, hl.reason as reason , count(hl.reason) as recordcount "
		  + "from harvestlog hl left join harvest h on hl.harvestid = h.harvestid left join contributor c on c.contributorid=h.contributorid "
		  + "where hl.errorlevel = :errorlevel AND c.collectionid = :collectionid AND h.starttime > :startdate AND h.starttime < :enddate "
		  + "group by c.name, hl.stepid, hl.reason"); //"group by c.name, h.harvestid, hl.stepid, hl.reason");
		
		reportQuery.setInteger("collectionid", collectionid);
		reportQuery.setDate("startdate", startdate);
		reportQuery.setDate("enddate", enddate);
		reportQuery.setInteger("errorlevel", HarvestLog.RECORD_ERROR);
		
		Object[] header = {"Contributor", "Processing Step", "Reason", "Record Count"};
		List<Object[]> report = reportQuery.list();
		report.add(0, header);
		return report;
	}
	
	@Transactional(readOnly=true)
	public List<Object[]> getRecordsByCollectionReport(int collectionid, Date startdate, Date enddate) {
		
		SQLQuery reportQuery = sf.getCurrentSession().createSQLQuery(
			   "select c.name, trunc(h.starttime, 'MONTH') as startmonth, "
			 + "sum(h.recordscompleted - h.recordsadded) as updated, sum(h.recordsadded) as added, "
			 + "sum(h.deletionsperformed) as deleted, sum(h.totalrecords - h.recordscompleted) as rejected, "
			 + "sum(h.recordsadded - h.deletionsperformed) as growth "
			 + "from contributor c inner join harvest h on c.contributorid = h.contributorid "
			 + "where collectionid=:collectionid AND h.starttime > :startdate AND h.starttime < :enddate AND h.type=1 " 
			 + "group by c.name, trunc(h.starttime, 'MONTH') order by c.name ASC, startmonth ASC");
		
		reportQuery.setInteger("collectionid", collectionid);
		reportQuery.setDate("startdate", startdate);
		reportQuery.setDate("enddate", enddate);
		
		Object[] header = {"Contributor", "Month", "Records Updated", "Records Added", "Records Deleted", "Records Rejected", "Record Growth"};
		List<Object[]> report = reportQuery.list();
		report.add(0, header);
		return report;
	}
	
	@Transactional(readOnly=true)
	public List<Object[]> getHarvestErrorsReport(int collectionid, Date startdate, Date enddate) {
		
		SQLQuery reportQuery = sf.getCurrentSession().createSQLQuery(
			   "select c.name, hl.reason, count(hl.errorlevel) as errorcount "
			 + "from harvestlog hl left join harvest h on h.harvestid = hl.harvestid left join contributor c on c.contributorid = h.contributorid "
			 + "where hl.errorlevel = 2 and collectionid = :collectionid AND h.starttime > :startdate AND h.starttime < :enddate "
			 + "group by c.name, hl.reason order by c.name ASC, hl.reason ASC");
		
		reportQuery.setInteger("collectionid", collectionid);
		reportQuery.setDate("startdate", startdate);
		reportQuery.setDate("enddate", enddate);
		
		Object[] header = {"Contributor", "Error", "Error Count"};
		List<Object[]> report = reportQuery.list();
		report.add(0, header);
		return report;
	}
	
	
}
