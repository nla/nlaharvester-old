package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.RecordDAO;
import harvester.client.util.WebUtil;
import harvester.data.*;

import java.util.*;

import org.apache.commons.logging.*;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unchecked")
public class HRecordDAO implements RecordDAO{


	protected final Log logger = LogFactory.getLog(HRecordDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	
	@Transactional(readOnly=true)
	public String getHarvestDataRecordData(int harvestdataid) throws Exception{			 
		HarvestData hd = (HarvestData) sf.getCurrentSession().get(HarvestData.class, harvestdataid);	
		 if(hd.getData() == null)
			 return null;
		return WebUtil.slurp(hd.getData().getBinaryStream());
	}

	@Transactional(readOnly=true)
	public String getHarvestLogRecordData(int harvestlogid) throws Exception{
		 HarvestLog hl = (HarvestLog) sf.getCurrentSession().get(HarvestLog.class, harvestlogid);	
		 if(hl.getRecorddata() == null)
			 return null;
		 return WebUtil.slurp(hl.getRecorddata().getBinaryStream());
	}

	@Transactional(readOnly=true)
	public List<Object> getHarvestLogs(int harvestid) {
		List logs = sf.getCurrentSession().createQuery("select new HarvestLog(harvestlogid, harvestid, timestamp, errorlevel, description, hasdata) " +
	 				"from HarvestLog as hl where hl.harvestid = " + harvestid + " order by hl.harvestlogid asc")
	 				.list();				
		return logs;
	}

	
}
