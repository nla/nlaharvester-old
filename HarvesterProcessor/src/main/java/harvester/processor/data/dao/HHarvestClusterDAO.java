package harvester.processor.data.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import harvester.data.*;
import harvester.processor.data.dao.interfaces.HarvestClusterDAO;
import harvester.processor.util.HibernateUtil;

public class HHarvestClusterDAO implements HarvestClusterDAO {

	private static Logger logger = Logger.getLogger(HHarvestLogDAO.class);
	
	public void saveHarvestCluster(HarvestCluster hc) {

		logger.info("saving cluster for harvestid=" + hc.getHarvestid() + " xpath=" + hc.getXpath());
		
		Session session  = null;
        try {
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		if(session.getTransaction().isActive())
			save(session, hc);
		else
		{
			session.beginTransaction();
			save(session, hc);
			session.getTransaction().commit();
		}
	} catch (HibernateException e) {
		//if(session != null)
		//	session.getTransaction().rollback();

		logger.error((new Date()).toString() + " | error logging harvest cluster:" + hc.getXpath(), e);
		if(session.getTransaction().isActive())
			session.getTransaction().rollback();
	}
		
	}

	private void save(Session session, HarvestCluster hc) {
		
		Set<HarvestClusterData> hcds = hc.getData();
		hc.setData(null);
		 
		int id = (Integer)session.save(hc);
		 
		for(HarvestClusterData hcd: hcds)
			hcd.setHarvestclusterid(id);
		
		hc.setData(hcds);
		
		session.persist(hc);
		
		 
	}

}
