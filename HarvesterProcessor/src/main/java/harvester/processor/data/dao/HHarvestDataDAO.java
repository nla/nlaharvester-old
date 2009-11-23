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

	public void AddToDatabase(HarvestData hd) throws Exception {

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


	public void AddToDatabaseBulk(LinkedList<Object> records, int harvestid, int stage) throws Exception {
		logger.info("saving records, harvestid=" + harvestid + " stage=" + stage);

		StatelessSession session  = null;
		try {
			session = HibernateUtil.getSessionFactory().openStatelessSession();
			Transaction tx = session.beginTransaction();

			//loop over each record
			for(Object rec : records) {
				HarvestData hd = new HarvestData();
				hd.setHarvestid(harvestid);
				hd.setStage(stage);	 

				//this should pretty print to document

				OutputFormat format = OutputFormat.createPrettyPrint();
				StringWriter out = new StringWriter();
				format.setEncoding("UTF-8");
				XMLWriter writer = new XMLWriter( out, format );
				writer.write((Document)rec);
				writer.close();
				out.close();
				String data = out.toString();

				hd.setData(Hibernate.createBlob(data.getBytes("UTF-8")));

				session.insert(hd);		 
			}


			logger.info("attempting commit");

			tx.commit();
			session.close();

			logger.debug("Completed commit");
		} catch (HibernateException e) {
			if(session != null)
				session.getTransaction().rollback();
			logger.error("bloody hibernate", e);
			throw new Exception("hibernate exception");
		}

	}

}
