package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.ContributorDAO;
import harvester.client.util.*;
import harvester.data.*;

import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.logging.*;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;


@SuppressWarnings("unchecked")
public class HContributorDAO implements ContributorDAO {

	protected final Log logger = LogFactory.getLog(HContributorDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributor(int contributorid, boolean collection, 
			boolean lastharvests, boolean harvests, boolean profiles, boolean detailedprofiles,
			boolean notes, boolean harveststage, boolean contacts) {
		
    	Contributor con = (Contributor)sf.getCurrentSession().load(Contributor.class, contributorid);
    	Hibernate.initialize(con);
    	
    	if(collection)
    		Hibernate.initialize(con.getCollection());
    	if(lastharvests) {
    		Hibernate.initialize(con.getLastharvest());
    		Hibernate.initialize(con.getLastsuccessfulprod());
    		Hibernate.initialize(con.getLastsuccessfultest());
    	}
    	if(harvests)
    		Hibernate.initialize(con.getHarvests());
    	if(profiles) {
    		Hibernate.initialize(con.getProduction());
    		Hibernate.initialize(con.getTest());
    		if(detailedprofiles) {
    			if(con.getProduction() != null)
    			{
    				Hibernate.initialize(con.getProduction().getProfilesteps());
    				for(ProfileStep ps : con.getProduction().getProfilesteps())
    					Hibernate.initialize(ps.getStep());
    			}
    			if(con.getTest() != null)
    			{
    				Hibernate.initialize(con.getTest().getProfilesteps());
    				for(ProfileStep ps : con.getTest().getProfilesteps())
    					Hibernate.initialize(ps.getStep());
    			}
    			Hibernate.initialize(con.getCollection().getProfiles());
    			Hibernate.initialize(con.getCollection().getLoadstage());
    			if(con.getCollection().getLoadstage() != null)
    				Hibernate.initialize(con.getCollection().getLoadstage().getStep());
    			if(con.getHarveststage() != null)
    				Hibernate.initialize(con.getHarveststage().getStep());
    		}
    	}
    	if(notes)
    		Hibernate.initialize(con.getNotes());
    	if(harveststage) {
    		Hibernate.initialize(con.getHarveststage());
    		if(con.getHarveststage() != null)
    		{
    			Hibernate.initialize(con.getHarveststage().getStep());
    			Hibernate.initialize(con.getHarveststage().getParameters());
    			Hibernate.initialize(con.getHarveststage().getStep().getPis());
    			
    			for(ProfileStepParameter p : con.getHarveststage().getParameters())
    				Hibernate.initialize(p.getPis());
    			for(ParameterInformation pi : con.getHarveststage().getStep().getPis())
    				Hibernate.initialize(pi.getOptions());
    		}
    	}
    	if(contacts) {
    		Hibernate.initialize(con.getContacts());		
    		Hibernate.initialize(con.getContactselections());
    		Hibernate.initialize(con.getCollection().getContacts());
    	}
    	return con;
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributor(int contributorid) {
		return getContributor(contributorid, true, false, false, false, false, false, false, false);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorLastHarvestsAndCollection(int contributorid) {
		return getContributor(contributorid, true, true, false, false, false, false, false, false);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorCollectionAndHarvests(int contributorid) {
		return getContributor(contributorid, true, true, true, false, false, false, false, false);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorCollectionAndDataprofile(int contributorid) {
		return getContributor(contributorid, true, false, false, true, false, false, false, false);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorCollectionAndDetailedDataprofile(int contributorid) {
		return getContributor(contributorid, true, false, false, true, true, false, false, false);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorCollectionAndNotes(int contributorid) {
		return getContributor(contributorid, true, false, false, false, false, true, false, false);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorAndHarvestStepDetails(int contributorid) {
		return getContributor(contributorid, true, false, false, false, false, false, true, false);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorCollectionContactsAndLastHarvest(int contributorid) {
		return getContributor(contributorid, true, true, false, false, false, false, false, true);
	}
	
	@Transactional(readOnly=true)
	public Contributor getContributorAndConnectionSettings(int contributorid) {
		return getContributor(contributorid, true, true, false, false, false, false, true, true);
	}

	@Transactional
	public void updateContributor(Contributor c) {
		sf.getCurrentSession().update(c);
	}
	
	@Transactional
	public void saveOrUpdateContributor(Contributor c) {
		logger.info("saveOrUpdate Called");
		logger.info("contributorid = " + c.getContributorid());
		logger.info("num contributorcontacts: " + c.getContacts().size());
		logger.info("num collection contacts: " + c.getContactselections().size());
		logger.info("name=" + c.getName());
		
		for(ContributorContact con : c.getContacts())
			if( con.getContactid() == -1){
				con.setContactid(null);
			}
		for(ContactSelections con : c.getContactselections())
			if( con.getSelectionid() == -1){
				con.setSelectionid(null);
			}
		
		if(c.getContributorid() == -1){
			logger.info("saving contributor.........");
			sf.getCurrentSession().save(c);
		} else {
			logger.info("merging contributor");
			sf.getCurrentSession().merge(c);	//changed from update to merge
		}
	
		sf.getCurrentSession().flush();
	}

	@Transactional
	public void addNewHarvestStage(int cid, int stepid, Map<Integer, String> parameters) {
		logger.info("creating new harvset stage for cid=" + cid + "stepid=" + stepid);
		
		//first create a new pipelinestage object
		ProfileStep ps = new ProfileStep();
		ps.setStep((Step)sf.getCurrentSession().get(Step.class, stepid));	//set the step it uses up
		
		Contributor c = (Contributor) sf.getCurrentSession().get(Contributor.class, cid);
		//if this contributor all ready has a pipeline stage we should just delete it
		
		ProfileStep oldps = c.getHarveststage();
		c.setHarveststage(null);
		ps.setContributorid(c.getContributorid());
		
		sf.getCurrentSession().flush();
		if(oldps != null)
			sf.getCurrentSession().delete(oldps);
		c.setHarveststage(ps);
		sf.getCurrentSession().save(ps);
		
		logger.info("saved new pipeline stage, adding parameters...");
		
		//now add each parameter
		ps.setParameters(new HashSet<ProfileStepParameter>());
		
		for(Entry<Integer, String> entry : parameters.entrySet())
		{
			ProfileStepParameter p = new ProfileStepParameter();
			p.setPss(ps);
			p.setValue(entry.getValue());
			p.setGrouplistindex(1);
			//get the associated parameterinfo object
			ParameterInformation pis = (ParameterInformation)sf.getCurrentSession().get(ParameterInformation.class, entry.getKey());
			
			logger.info("saving parameter key=" + entry.getKey() + " value=" + entry.getValue() + "pis name=" + pis.getParametername());
			
			p.setPis(pis);
			ps.getParameters().add(p);
			
			sf.getCurrentSession().save(p);
		}		

		//these might not be needed, but can't harm
		sf.getCurrentSession().update(c);
		sf.getCurrentSession().update(ps);
	}

	@Transactional
	public void togglemonitored(int contributorid) {
    	Contributor con = (Contributor)sf.getCurrentSession().get(Contributor.class, contributorid);
    	if(con.getIsmonitored() == 1)
    		con.setIsmonitored(0);
    	else
    		con.setIsmonitored(1);
	}
	
	@Transactional
	public void toggleproduction(int contributorid) {
    	Contributor con = (Contributor)sf.getCurrentSession().get(Contributor.class, contributorid);
    	if(con.getType() == 0)
    		con.setType(1);
    	else
    		con.setType(0);
	}
	
	@Transactional(readOnly=true)
	public boolean isNameInUse(String name, String collectionid){
		try{
			Query query = sf.getCurrentSession().createQuery(
					"select c.name from Contributor c where c.collection.collectionid = :id AND c.name= :name ");
			query.setString("name", name);
			query.setString("id", collectionid);
			List contributors = query.list();
			return (contributors != null && !contributors.isEmpty());
		} catch(Exception e) {
			logger.error("Error was thrown in isNameInUse:" + e.getMessage());
			return false;
		}
	}
	
	@Transactional(readOnly=true)
	public Date getLastProductionHarvestDate(int contributorid) {
			List harvests = sf.getCurrentSession().createQuery(
					"from Harvest h where h.contributor.contributorid=" + contributorid + " AND h.type=1 ORDER BY h.starttime DESC LIMIT 1").list();
		if(!harvests.isEmpty())
			return ((Harvest)harvests.get(0)).getStarttime();
		else
			return null;
	}

	@Transactional
	public void deletecontributor(int contributorid) {
		Contributor c = (Contributor) sf.getCurrentSession().get(Contributor.class, contributorid);
		sf.getCurrentSession().createSQLQuery(
    			"DELETE FROM harvestdata WHERE harvestid IN " +
    			"(SELECT DISTINCT harvestid from harvest h where contributorid = " + contributorid + ")")
    			.executeUpdate();
		sf.getCurrentSession().createSQLQuery(
    			"DELETE FROM harvestlog WHERE harvestid IN " +
    			"(SELECT DISTINCT harvestid from harvest h where contributorid = " + contributorid + ")")
    			.executeUpdate();	
		sf.getCurrentSession().createSQLQuery(
				"DELETE FROM harvestclusterdata where harvestclusterid IN" + 
				"(SELECT DISTINCT harvestclusterid from harvest natural join harvestcluster where contributorid = " + contributorid + ")")
				.executeUpdate();
		sf.getCurrentSession().createSQLQuery(
				"DELETE FROM harvestcluster WHERE harvestid IN " +
    			"(SELECT DISTINCT harvestid from harvest h where contributorid = " + contributorid + ")")
    			.executeUpdate();			
		sf.getCurrentSession().delete(c);
	}

	@Transactional
	public void doRemoveRecords(int contributorid, int type) {
		sf.getCurrentSession().createSQLQuery(
    			"DELETE FROM harvestdata WHERE harvestid IN " +
    			"(SELECT DISTINCT harvestid from harvest h where contributorid = " + contributorid + " AND h.type = " + type + ")")
    			.executeUpdate();
	}

	@Transactional
	public void setHidden(Integer contributorid) {
		Contributor c = (Contributor) sf.getCurrentSession().get(Contributor.class, contributorid);
		c.setHidefromworktray(1);
	}

	@Transactional
	public void setRecordCount(int contributorid, int count) {
		Contributor c = (Contributor) sf.getCurrentSession().get(Contributor.class, contributorid);
		c.setTotalrecords(count);
	}
	
	
	
}
