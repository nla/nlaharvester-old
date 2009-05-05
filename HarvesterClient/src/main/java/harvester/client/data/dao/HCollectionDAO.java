package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.CollectionDAO;
import harvester.client.util.KeyValue;
import harvester.data.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unchecked")
public class HCollectionDAO implements CollectionDAO {
	
	protected final Log logger = LogFactory.getLog(HCollectionDAO.class);
	private SessionFactory sf;

	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}

	@Transactional(readOnly=true)
	public List<Collection> getAllCollections(){
        return (List<Collection>) sf.getCurrentSession()
        		.createSQLQuery("select * from collection c order by UPPER(c.name)").addEntity(Collection.class).list();
	}

	@Transactional(readOnly=true)
	public Collection getCollection(int collectionid, boolean contacts, boolean dependents, boolean deepprofiles,
			boolean contributors){
    	Collection c = (Collection)sf.getCurrentSession().load(Collection.class, collectionid);
    	Hibernate.initialize(c);
    	if(c.getLoadstage() != null)
    		Hibernate.initialize(c.getLoadstage().getStep());
    	
    	if(contacts)
        	Hibernate.initialize(c.getContacts());
    	if(dependents) {
    		Hibernate.initialize(c.getProfiles());
    		Hibernate.initialize(c.getLoadstage());
    	}
    	if(contributors) {
    		Hibernate.initialize(c.getContributors());
    		for(Contributor con : c.getContributors())
    		{
    			Hibernate.initialize(con.getLastharvest());
    		}
    	}
    	if(deepprofiles) {
    		for(Profile p : c.getProfiles()) {
    			for(ProfileStep ps : p.getProfilesteps()) {
    			Hibernate.initialize(ps.getStep());
    			}
    		}
    	}
    	
    	return c;
	}
	
	@Transactional(readOnly=true)
	public Collection getCollection(int collectionid){
		return getCollection(collectionid, false, false, false, false);
	}
	
	@Transactional(readOnly=true)
	public Collection getCollectionAndContacts(int collectionid){
		return getCollection(collectionid, true, false, false, false);
	}
	
	@Transactional(readOnly=true)
	public Collection getCollectionContributorsAndLastHarvests(int collectionid)
	{
		return getCollection(collectionid, false, false, false, true);
	}

	@Transactional(readOnly=true)
	public Collection getCollectionAndDependents(int collectionid){	
		return getCollection(collectionid, true, true, false, false);
	}
	
	@Transactional(readOnly=true)
	public Collection getCollectionAndProfiles(int collectionid){	
		return getCollection(collectionid, false, false, true, false);
	}

	@Transactional
	public Collection saveOrUpdateCollection(Collection c, Integer psid) {
		logger.info("EditCollections transaction started");		
		logger.info("psid=" + psid);
		
		if(psid != null) {
			ProfileStep loadstage = (ProfileStep)sf.getCurrentSession().get(ProfileStep.class, psid);
			c.setLoadstage(loadstage);
		} else 
			c.setLoadstage(null);
		
		for(CollectionContact con : c.getContacts()){
			if( con.getContactid() == -1)
				con.setContactid(null);
		}
		
		//collection
		if(c.getCollectionid() <= 0){
			logger.info("saving collection.........");
			sf.getCurrentSession().save(c);
		} else
			sf.getCurrentSession().merge(c);

		sf.getCurrentSession().flush();
		
		return c;
	}
	
	@Transactional
	public void modifyCollection(Collection c) {
		sf.getCurrentSession().merge(c);
	}
	

	@Transactional(readOnly=true)
	public boolean isNameInUse(String name) {
		try{
			Query query = sf.getCurrentSession().createQuery("select c.name from Collection c where c.name= :name ");
			query.setString("name", name);
			List collections = query.list();
			return (collections != null && !collections.isEmpty());
		} catch(Exception e) {
			logger.error("Error was thrown in isNameInUse:" + e.getMessage());
			return false;
		}
	}

	@Transactional(readOnly=true)
	public LinkedList<KeyValue> getDefaultProfilesForCollection(int collectionid, int htype) {
		
		Collection c = (Collection)sf.getCurrentSession().get(Collection.class, collectionid);
		LinkedList<KeyValue> kvs = new LinkedList<KeyValue>();
		for(Profile p : c.getProfiles()) {
			if(p.getType() == htype)
				kvs.add(new KeyValue(String.valueOf(p.getProfileid()), p.getName() ));
		}
		
		return kvs;
	}
	
	@Transactional(readOnly=true)
	public int getContributorCount(int collectionid) {
		Query q = sf.getCurrentSession().createQuery("select count(c) from Contributor c where collectionid=" + collectionid);
		Object result = q.uniqueResult();
		return Integer.valueOf(result.toString());
	}
	
	@Transactional
	public void deleteCollection(int collectionid) {
		Collection c = (Collection) sf.getCurrentSession().get(Collection.class, collectionid);
		sf.getCurrentSession().delete(c);
	}
	
	@Transactional(readOnly=true)
	public List<ProfileStep> getPossibleOutputStages()
	{
		//type 1 is a step of type "input", we will do a direct sql query for simplicity
		SQLQuery q = sf.getCurrentSession()
			.createSQLQuery("SELECT * FROM profilestep WHERE stepid IN " +
					"(SELECT stepid FROM step WHERE type=1)")
			.addEntity(ProfileStep.class);
		return q.list();
	}
	
}