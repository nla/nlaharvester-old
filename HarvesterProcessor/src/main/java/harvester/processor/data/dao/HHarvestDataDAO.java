package harvester.processor.data.dao;

import harvester.data.Harvest;
import harvester.data.HarvestData;
import harvester.processor.data.dao.interfaces.HarvestdataDAO;
import harvester.processor.util.HibernateUtil;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.hibernate.*;

import org.dom4j.Document;
import org.dom4j.io.*;


public class HHarvestDataDAO implements HarvestdataDAO
{

	private static Logger logger = Logger.getLogger(HHarvestDataDAO.class);
	
	public void AddToDatabase(HarvestData hd) throws Exception 
	{
		
		Session session  = null;
        try {
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		 session.beginTransaction();
		 
		 session.save(hd);
		 session.getTransaction().commit();
		 logger.debug("New harvestdata item added to database.   id=" + hd.getHarvestdataid());
		} catch (HibernateException e) {
			if(session != null)
				session.getTransaction().rollback();
			throw new Exception("hibernate exception");
		}

	}
	
	
	public void AddToDatabaseBulk(LinkedList<Object> records, int harvestid, int stage) throws Exception 
	{
		logger.info("saving records, harvestid=" + harvestid + " stage=" + stage);
		
		Session session  = null;
        try {
		session = HibernateUtil.getSessionFactory().getCurrentSession();
		 session.beginTransaction();
		 
		 int count = 0;
		 
		 //Harvest h = (Harvest)session.get(Harvest.class, harvestid);
		 
		 //loop over each record
		 for(Object rec : records)
		 {

			 HarvestData hd = new HarvestData();
			 //if(id != null)
			//	 hd.setHarvestdataid(Integer.valueOf(id));
			 hd.setHarvestid(harvestid);
			 hd.setStage(stage);
			// h.getHarvestdata().add(hd);			 
			 
			 //this should pretty print to document

			 OutputFormat format = OutputFormat.createPrettyPrint();
			 StringWriter out = new StringWriter();
			 format.setEncoding("UTF-8");
			 XMLWriter writer = new XMLWriter( out, format );
			 writer.write((Document)rec);
			 writer.close();
			 out.close();
			 String data = out.toString();

			 //hd.setData(Hibernate.createClob(data));
			 hd.setData(Hibernate.createBlob(data.getBytes("UTF-8")));
			 //hd.setBdata(Hibernate.createBlob("testing 123".getBytes("UTF-8")));
			 
			 session.save(hd);	
			 
			 count++;
			 if(count % 20 == 0)
			 {
				 session.flush();
				 session.clear();
			 }
		 }

		 session.flush();
		 session.clear();
		 
		 logger.info("attempting commit, count = " + count + "...");
		 session.getTransaction().commit();
		 
		 logger.debug("Completed commit");
		} catch (HibernateException e) {
			if(session != null)
				session.getTransaction().rollback();
			logger.error("bloody hibernate", e);
			throw new Exception("hibernate exception");
		}

	}

}
