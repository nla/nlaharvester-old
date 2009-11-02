package harvester.processor.data.dao;

import harvester.processor.email.HarvestError;
import harvester.data.HarvestLog;
import harvester.processor.data.dao.interfaces.HarvestlogDAO;
import harvester.processor.util.HibernateUtil;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;


public class HHarvestLogDAO implements HarvestlogDAO{
	
	private static Logger logger = Logger.getLogger(HHarvestLogDAO.class);
	
	public void AddToDatabase(HarvestLog hl) {
		
		Session session  = null;
        try {
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		if(session.getTransaction().isActive())
		 session.save(hl);
		else
		{
			session.beginTransaction();
			
			session.save(hl);
			
			session.getTransaction().commit();
		}
	} catch (HibernateException e) {
		logger.error((new Date()).toString() + " | error logging:" + hl.getDescription());
		if(session.getTransaction().isActive())
			session.getTransaction().rollback();
	}

	}

	
	public List<HarvestError> getErrorSummary(int harvestid) {
		SQLQuery reportQuery = HibernateUtil.getSessionFactory().getCurrentSession().createSQLQuery(
			"select (select name from step where stepid = hl.stepid) as step, hl.reason as reason , count(hl.reason) as recordcount "
		  + "from harvestlog hl left join harvest h on hl.harvestid = h.harvestid  "
		  + "where hl.errorlevel = :errorlevel AND h.harvestid=:harvestid  "
		  + "group by hl.stepid, hl.reason");
		
		reportQuery.setInteger("harvestid", harvestid);

		reportQuery.setInteger("errorlevel", HarvestLog.RECORD_ERROR);
		
		List<Object[]> report = reportQuery.list();
		
		LinkedList<HarvestError> errors = new LinkedList<HarvestError>();
		
		for(Object[] row : report) {
			HarvestError he = new HarvestError();
			if(row[0] != null) he.setStepName(row[0].toString());
			
			if(row[1] != null)he.setError(row[1].toString());
			
			he.setRecordCount(Integer.valueOf(row[2].toString()));
			errors.add(he);
		}
		
		return errors;
	}
	
}
