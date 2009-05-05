package harvester.processor.data.dao;

import harvester.data.ParameterOption;
import harvester.processor.data.dao.interfaces.ParameteroptionDAO;
import harvester.processor.util.*;

import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Session;


public class HParameterOptionDAO implements ParameteroptionDAO {

	private static Logger logger = Logger.getLogger(HParameterOptionDAO.class);
	
	public void updateOptions(HashMap<String, ParameterOption> pos, Integer piid)
			throws Exception
	{
		
		logger.info("attempting to save gathered parameteroptions count is " + pos.size());
		
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		//get the current parameter options for this piid
		
		List currentpos = session.createQuery("from ParameterOption as po where po.piid = " + piid).list();
		
		logger.info("current number is " + currentpos.size());
		
		for(Object ocpo : currentpos)
		{
			ParameterOption cpo = (ParameterOption)ocpo;
			logger.info("considering : " + cpo.getValue());
			
			//we need to see if this one is in the pos hashmap, if it is not we delete it
			ParameterOption match = pos.get(cpo.getValue());
			if(match == null)
			{
				logger.info("deleting option = " + cpo.getValue());
				session.delete(cpo);				
			} else
			{
				pos.remove(cpo.getValue());
			}			
		}
		
		//any that are left in the hashmap should be added now
		
		for(String key : pos.keySet())
		{
			logger.info("added new option key = " + key);
			session.save(pos.get(key));
		}
		
		logger.info("attempting commit...");
		session.getTransaction().commit();
		logger.info("Successfully saved parameter options");
		
	}

}
