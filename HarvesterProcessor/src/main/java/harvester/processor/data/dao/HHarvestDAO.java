package harvester.processor.data.dao;

import harvester.data.Contributor;
import harvester.data.Harvest;
import harvester.data.Profile;
import harvester.processor.data.dao.interfaces.HarvestDAO;
import harvester.processor.util.HibernateUtil;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;


public class HHarvestDAO implements HarvestDAO{

	private static Logger logger = Logger.getLogger(HHarvestDAO.class);
	
	public void ApplyChanges(Harvest h) throws Exception {
		
		 Session session = null;
			try
			{
		        fixlongstatus(h);
		        session = HibernateUtil.getSessionFactory().getCurrentSession();
		        session.beginTransaction();
			 
		        session.merge(h);
			 
		        logger.debug("harvest updated.   id=" + h.getHarvestid());
				session.getTransaction().commit();
			}
			catch(Exception e)
			{
				if(session != null)
					session.getTransaction().rollback();
				throw new Exception("hibernate exception", e);
			}
		
	}
	
	private void fixlongstatus(Harvest h) {

		if(h.getStatus().length() > 255)
			h.setStatus(h.getStatus().substring(0, 255));	
	}

	public void makeLastSuccHarvest(int hid, int cid, int type) throws Exception
	{
		 Session session = null;
			try
			{	
			     session = HibernateUtil.getSessionFactory().getCurrentSession();
				 session.beginTransaction();
				 
				 Contributor c = (Contributor)session.load(Contributor.class, cid);
				 Harvest h = (Harvest)session.load(Harvest.class, hid);
				 
				 if(type == Profile.TEST_PROFILE)
				 {
					 c.setLastsuccessfultest(h);
				 }
				 else
				 {
					 c.setIsfinishedfirstharvest(Contributor.NOT_FIRST_HARVEST);
					 c.setLastsuccessfulprod(h);
				 }
				 
				 
				 session.getTransaction().commit();
				 
				 logger.debug("last successful harvest info updated.   hid=" + h.getHarvestid() + "cid=" + c.getContributorid());
			}
			catch(Exception e)
			{
				if(session != null)
					session.getTransaction().rollback();
				throw e;
			}
	}

	public void AddToDatabase(Harvest h) throws Exception{
		Session session  = null;
        try {
	    fixlongstatus(h);
	    
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		 session.beginTransaction();
		 
		 session.save(h);
		 session.getTransaction().commit();
		 logger.debug("New harvest added to database.   id=" + h.getHarvestid());
	} catch (HibernateException e) {
		if(session != null)
			session.getTransaction().rollback();
		throw new Exception("hibernate exception", e);
	}
		
	}

}
