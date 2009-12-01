package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.RecordDAO;
import harvester.client.util.WebUtil;
import harvester.data.*;

import java.util.*;

import org.apache.commons.logging.*;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unchecked")
public class HRecordDAO implements RecordDAO{


	protected final Log logger = LogFactory.getLog(HRecordDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	
	@Transactional(readOnly=true)
	public String getHarvestDataRecordData(int harvestdataid) throws Exception{			 
		HarvestData hd = (HarvestData) sf.getCurrentSession().get(HarvestData.class, harvestdataid);	
		 if(hd.getData() == null)
			 return null;
		return WebUtil.slurp(hd.getData().getBinaryStream());
	}

	@Transactional(readOnly=true)
	public String getHarvestLogRecordData(int harvestlogid) throws Exception{
		 HarvestLog hl = (HarvestLog) sf.getCurrentSession().get(HarvestLog.class, harvestlogid);	
		 if(hl.getRecorddata() == null)
			 return null;
		 return WebUtil.slurp(hl.getRecorddata().getBinaryStream());
	}

	@Transactional(readOnly=true)
	public List<Object> getHarvestLogs(int harvestid) {
		List logs = sf.getCurrentSession().createQuery("select new HarvestLog(harvestlogid, harvestid, timestamp, errorlevel, description, hasdata) " +
	 				"from HarvestLog as hl where hl.harvestid = " + harvestid + " order by hl.harvestlogid asc")
	 				.list();				
		return logs;
	}
	
	@Transactional(readOnly=true)
	public List<Object> getHarvestLogsWithErrorLevel(int harvestid, int error_level) {
		List logs = sf.getCurrentSession().createQuery(
				"select new HarvestLog(harvestlogid, harvestid, timestamp, errorlevel, description, hasdata) " +
 				"from HarvestLog as hl where hl.harvestid = " + harvestid + 
 				" and hl.errorlevel = " + error_level +  
 				" order by hl.harvestlogid asc").list();	
		return logs;
	}
	
	@Transactional(readOnly=true)
	public List<Object> getLastHarvestLogs(int harvestid, int num_logs) {
		Query q = sf.getCurrentSession().createQuery(
				"select new HarvestLog(harvestlogid, harvestid, timestamp, errorlevel, description, hasdata) " +
 				"from HarvestLog as hl where hl.harvestid = " + harvestid + " and hl.errorlevel <> " + HarvestLog.PROP_INFO +  
 				" order by hl.harvestlogid desc");
		
		q.setMaxResults(num_logs);
		
		List logs = q.list();
		Collections.reverse(logs);
		
		return logs;
	}
	
	//return a list of object arrays, (num, errors)
	@Transactional(readOnly=true)
	public List<Object> getPlaceholders(int harvestid, int last_record, int chunksize) {
		
		String first_log_id_sql = "select min(hl.harvestlogid) from HarvestLog as hl where hl.harvestid=:harvestid";
		
		Query first_log_query = sf.getCurrentSession().createQuery(first_log_id_sql).setInteger("harvestid", harvestid);
		Object first_log_id = first_log_query.uniqueResult();
		
					    //amount in chunk
		String query = "select count(harvestid) as num," + 
						// count the number of error records within that chunk. Decode is basically a switch statement.
					   " count(decode(errorlevel, " + HarvestLog.INFO + ", null, 1))" + 
					   " as errors from harvestlog where harvestid=" + harvestid + " and harvestlogid < " + last_record + 
					   " and errorlevel <> " + HarvestLog.PROP_INFO + 
					   //group into chunks of chunk_size
					   " group by round((harvestlogid - " + first_log_id + ")/" + chunksize + ")";
		
		SQLQuery q = sf.getCurrentSession().createSQLQuery(query);
		q.addScalar( "num", Hibernate.INTEGER); 
		q.addScalar( "errors", Hibernate.INTEGER);

		return q.list();
	}
	
	@Transactional(readOnly=true)
	public List<Object> getVisibleHarvestLogsInRange(int harvestid, int start, int end) {
		Query q = sf.getCurrentSession().createQuery(
					"select new HarvestLog(harvestlogid, harvestid, timestamp, errorlevel, description, hasdata) " +
	 				"from HarvestLog as hl where hl.harvestid = " + harvestid + 
	 				" and hl.errorlevel <> " + HarvestLog.PROP_INFO + " and hl.errorlevel <> " + HarvestLog.REPORT_INFO + 
	 				" order by hl.harvestlogid asc");	
		
		q.setFirstResult(start);
		q.setMaxResults(end-start);
		
		return q.list();
	}


	@Transactional(readOnly=true)
	public List<Object> getNewHarvestLogs(int harvestid, Date fromdate) {
		Query q = sf.getCurrentSession().createQuery("select new HarvestLog(harvestlogid, harvestid, timestamp, errorlevel, description, hasdata) " +
	 				"from HarvestLog as hl where hl.harvestid = :harvestid AND hl.timestamp > :fromdate order by hl.harvestlogid asc");
		q.setInteger("harvestid", harvestid);
		q.setTimestamp("fromdate", fromdate);
		return q.list();
	}
	
}
