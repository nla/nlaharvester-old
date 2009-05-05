package harvester.client.data.dao;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;

import harvester.client.data.dao.interfaces.StepFileDAO;
import harvester.client.util.WebUtil;
import harvester.data.Collection;
import harvester.data.Contributor;
import harvester.data.StepFile;

public class HStepFileDAO implements StepFileDAO {

	protected final Log logger = LogFactory.getLog(HContributorDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	@Transactional(readOnly=true)
	public StepFile getFile(int fileid) {
    	StepFile file = (StepFile)sf.getCurrentSession().load(StepFile.class, fileid);
    	Hibernate.initialize(file);
    	return file;
	}

	@Transactional
	public void saveOrUpdateFile(StepFile file) {
		sf.getCurrentSession().saveOrUpdate(file);
	}

	@Transactional(readOnly=true)
	public String getFileData(int fileid) throws Exception {
    	StepFile file = (StepFile)sf.getCurrentSession().load(StepFile.class, fileid);		
		return WebUtil.slurp(file.getData().getBinaryStream());
	}	

	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public List<StepFile> getFiles(int stepid) {
		return (List<StepFile>) sf.getCurrentSession()
			.createSQLQuery("select * from stepfile f where f.stepid = " + String.valueOf(stepid))
			.addEntity(StepFile.class).list();
	}

	@Transactional
	public void deleteFile(int fileid) {
		StepFile file = (StepFile)sf.getCurrentSession().load(StepFile.class, fileid);	
		sf.getCurrentSession().delete(file);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public List<String> getClashes(int fileid, int fileidid) {

		String query = "select name from contributor, " + 
							"(select * from profilestep where psid in " +
									"(select psid from profilestepparameter where piid = " + fileidid + " AND value = '" + fileid + "')) p " +  
						"where testid=p.profileid OR productionid = p.profileid order by name desc";
		
		List<Object> results = sf.getCurrentSession().createSQLQuery(query)
			.addScalar("name", Hibernate.STRING).list();
		
		logger.info("results : " + results.toString());
		
		List<String> names = new LinkedList<String>();
		
		for(Object result : results) {
			names.add(result.toString());
		}
		
		return names;
		
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public List<String> getCollectionClashes(int fileid, int fileidid) {
		
		String query = "select name from collection c, collectionprofile cp , " + 
							"(select profileid from profilestep where psid in " +
									"(select psid from profilestepparameter where piid = " + fileidid + " AND value = '" + fileid + "')) p " +  
						"where c.collectionid = cp.collectionid AND cp.profileid = p.profileid order by name desc";
		
		List<Object> results = sf.getCurrentSession().createSQLQuery(query)
			.addScalar("name", Hibernate.STRING).list();
		
		logger.info("results : " + results.toString());
		
		List<String> names = new LinkedList<String>();
		
		for(Object result : results) {
			names.add(result.toString());
		}
		
		return names;
		
	}

}
