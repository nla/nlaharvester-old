package harvester.client.data.dao;
import harvester.client.data.dao.interfaces.HarvestDAO;
import harvester.client.harvest.HarvestError;
import harvester.client.util.*;
import harvester.data.*;

import java.util.*;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;

import org.apache.commons.logging.*;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

@SuppressWarnings("unchecked")
public class HHarvestDAO implements HarvestDAO{
	
	protected final Log logger = LogFactory.getLog(HHarvestDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	@Transactional(readOnly=true)
	public Harvest getHarvest(int harvestid, boolean logs, boolean data) {
		Harvest h = (Harvest)sf.getCurrentSession().load(Harvest.class, harvestid);
		
		Hibernate.initialize(h.getContributor());
		Hibernate.initialize(h.getContributor().getCollection());
		
		if(logs)
			Hibernate.initialize(h.getHarvestlogs());
		if(data)
			Hibernate.initialize(h.getHarvestdata());
		
		return h;
	}
	
	@Transactional(readOnly=true)
	public Harvest getHarvestContributorCollectionAndLogs(int harvestid) {
		return getHarvest(harvestid, true, false);
	}
	
	@Transactional(readOnly=true)
	public Harvest getHarvestContributorCollection(int harvestid) {
		return getHarvest(harvestid, false, false);
	}
	
	@Transactional(readOnly=true) //TODO: is this the same as the above function 
	public Harvest getHarvestAndContributor(Integer harvestid) {
		return getHarvest(harvestid, false, false);
	}
	
	@Transactional(readOnly=true)
	public Harvest getHarvestContributorCollectionAndData(Integer harvestid) {
		return getHarvest(harvestid, false, true);
	}
	
	@Transactional(readOnly=true)
	public void getStreamingHarvestData(int harvestid, ServletOutputStream os) throws Exception{

		StatelessSession session = sf.openStatelessSession();
		Transaction tx = session.beginTransaction();
		
		os.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		os.print("\n <records>");
		
		ScrollableResults items = session.createQuery("from HarvestData where harvestid=" + harvestid).scroll(ScrollMode.FORWARD_ONLY);
		
		Pattern ph = Pattern.compile("<\\?(.*?)\\?>");		
		
		while (items.next())
		{
			HarvestData record = (HarvestData) items.get(0);
			String data = WebUtil.slurp(record.getData().getBinaryStream());
			//records usuall have a xml header at the start
			String filtered = ph.matcher(data).replaceFirst("");
			os.write(filtered.getBytes("UTF-8"));
			//os.print(data);
			os.println();
		}
		
		os.print("</records>");
		
		tx.commit();
		session.close();
	}
	
	@Transactional(readOnly=true)
	public List<HarvestDataView> getHarvestDataWithPaging(int page, int harvestid, int pagesize) {

		Query q = sf.getCurrentSession().createQuery("from HarvestData where harvestid=" + harvestid);
		q.setFirstResult(pagesize*page);
		q.setMaxResults(pagesize);
	
		List qresult = q.list();
		Iterator iter = qresult.iterator();
		List<HarvestDataView> rows = new LinkedList();
		
		while(iter.hasNext())
		{
			HarvestData hd = (HarvestData) iter.next();
			
			HarvestDataView hdv = new HarvestDataView();
			hdv.setHarvestdataid(hd.getHarvestdataid());
			hdv.setStage(hd.getStage());
			
			try
			{				
				hdv.setData(WebUtil.slurp(hd.getData().getBinaryStream()));				
			} catch (Exception e)
			{
				logger.info("could not get a records data!");
			}
			
			rows.add(hdv);
		}
		return rows;
	}

	@Transactional(readOnly=true)
	public int getTotalDataRecords(int harvestid) {
		Query q = sf.getCurrentSession().createQuery("select count(*) from HarvestData hd where harvestid=" + harvestid);
		Object result = q.uniqueResult();
		return Integer.valueOf(result.toString());
	}
	
	@Transactional(readOnly=true)
	public int getTotalLogMessages(int harvestid) {
		Query q = sf.getCurrentSession().createQuery("select count(*) from HarvestLog hl where hl.harvestid=" + 
													 harvestid + " and hl.errorlevel <> " + HarvestLog.PROP_INFO);
		Object result = q.uniqueResult();
		return Integer.valueOf(result.toString());
	}
	
	
	@Transactional
	public void doHardHarvestStop(Integer harvestid) {
  		Harvest h = (Harvest)sf.getCurrentSession().load(Harvest.class, harvestid);
		h.setStatus("Stopped");
		h.setStatuscode(Harvest.SUCCESSFUL);
	}

	@Transactional
	public List<HarvestCluster> getClusters(int harvestid) {
		Query q = sf.getCurrentSession().createQuery("from HarvestCluster where harvestid=" + harvestid);
	
		List<HarvestCluster> qresult = q.list();
		
		for(HarvestCluster hc : qresult)
			Hibernate.initialize(hc.getData());
			
		return qresult;
	}

	@Transactional
	public boolean hasClusters(int harvestid) {
		Query q = sf.getCurrentSession().createQuery("select count(*) from HarvestCluster hc where harvestid=" + harvestid);
		Object count = q.uniqueResult();
		return Integer.valueOf(count.toString()) != 0;
	}

	@Transactional
	public void deleterecords(Integer harvestid) {  	
		sf.getCurrentSession().createSQLQuery("DELETE FROM harvestdata WHERE harvestid =" + harvestid).executeUpdate();    	            		
	}
	
	@Transactional(readOnly=true)
	public List<HarvestError> getErrorSummary(int harvestid) {
		SQLQuery reportQuery = sf.getCurrentSession().createSQLQuery(
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
