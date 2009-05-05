package harvester.processor.data.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import harvester.data.StepFile;
import harvester.processor.data.dao.interfaces.StepFileDAO;
import harvester.processor.util.*;

public class HStepFileDAO implements StepFileDAO {

	private static Logger logger = Logger.getLogger(HStepFileDAO.class);

	public String getFileData(int fileid) {
		
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		
		try {
			//session.beginTransaction();
			
	    	StepFile file = (StepFile)session.load(StepFile.class, fileid);		
			return StreamUtil.slurp(file.getData().getBinaryStream());
			
		} catch (Exception e)
		{
			logger.info("failed get", e);
		}
		
		return null;	

	}

	@SuppressWarnings("unchecked")
	public List<StepFile> getFiles(int stepid) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		
		try {
			//session.beginTransaction();
			
			List<StepFile> files = (List<StepFile>)session
			.createSQLQuery("select * from stepfile f where f.stepid = " + String.valueOf(stepid))
			.addEntity(StepFile.class).list();
			for(StepFile file : files)
				Hibernate.initialize(file);
			return files;
			
		} catch (Exception e) {
			logger.info("failed get", e);
		}
		
		return null;	
	}
	
}
