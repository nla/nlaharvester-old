package harvester.processor.data.dao;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import harvester.data.Step;
import harvester.processor.data.dao.interfaces.StepDAO;
import harvester.processor.util.HibernateUtil;

public class HStepDAO implements StepDAO {

	private static Logger logger = Logger.getLogger(HStepDAO.class);
	
	
	public String getStepClassName(int stepid) {
	
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		String stepName = null;
		
		try {
			session.beginTransaction();
			
			stepName = ((Step)session.get(Step.class, stepid)).getClassname();
			
			logger.info("attempting commit...");
			session.getTransaction().commit();
		} catch (Exception e) {	
		} 
		return stepName;
	}

}
