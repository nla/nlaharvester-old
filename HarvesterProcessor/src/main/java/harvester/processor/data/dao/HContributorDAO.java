package harvester.processor.data.dao;

import harvester.data.Contributor;
import harvester.processor.data.dao.interfaces.ContributorDAO;
import harvester.processor.util.HibernateUtil;

import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.Session;


public class HContributorDAO implements ContributorDAO {

	public Contributor getContributor(int contributorid) {
		return (Contributor) HibernateUtil.getSessionFactory()
		.getCurrentSession().load( Contributor.class, contributorid);
	}

	
	public void setTotalRecords(int contributorid, int recordsincollection, int recordsfromcontributor) throws Exception
	{
		Session session  = null;
        try {
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		 session.beginTransaction();

		 Contributor c = (Contributor)session.load(Contributor.class, contributorid);
		 
		 c.setTotalrecords(recordsfromcontributor);
		 
		 c.getCollection().setSize(recordsincollection);
		 
		 session.getTransaction().commit();
	} catch (HibernateException e) {
		if(session != null)
			session.getTransaction().rollback();
		throw new Exception("hibernate exception when changin total records stuff");
	}
		
	}
	
}
