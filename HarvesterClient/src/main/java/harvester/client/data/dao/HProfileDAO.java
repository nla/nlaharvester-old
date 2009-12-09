package harvester.client.data.dao;
import org.springframework.transaction.annotation.Transactional;

import harvester.client.data.dao.interfaces.ProfileDAO;
import harvester.data.*;

import org.apache.commons.logging.*;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Required;


@SuppressWarnings("unchecked")
public class HProfileDAO implements ProfileDAO {

	protected final Log logger = LogFactory.getLog(HProfileDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}

	@Transactional(readOnly=true)
	public Profile getProfile(int profileid) {
		return getProfileWithOptions(profileid, false);
	}
	@Transactional(readOnly=true)
	public Profile getProfileAndEvict(int dataprofileid)
	{
		return getProfileWithOptions(dataprofileid, true);
	}

	private Profile getProfileWithOptions(int dpid, boolean evict) {
		
		logger.info("getting dataprofile");
		Profile dp = (Profile)sf.getCurrentSession().load(Profile.class, dpid);
		
		for(ProfileStep ps : dp.getProfilesteps())
		{
			Hibernate.initialize(ps.getStep());
			for(ParameterInformation pi : ps.getStep().getPis())
			{
				Hibernate.initialize(pi);
				Hibernate.initialize(pi.getOptions());
        		Hibernate.initialize(pi.getNested());
        		for(ParameterInformation npi : pi.getNested())
        			Hibernate.initialize(npi.getOptions());
			}
			for(ProfileStepParameter p : ps.getParameters())
			{
				Hibernate.initialize(p);
				Hibernate.initialize(p.getPis());
				Hibernate.initialize(p.getPis().getNested());
			}
		}
		
		logger.info("complete");
		if(evict == true)
		{
			logger.info("evicting dataprofile from session cache");
			sf.getCurrentSession().evict(dp);
			logger.info("evicted");
		}
		return dp;
	}
	
	@Transactional
	public void saveCollectionProfile(Profile dp, int collectionid) {
		
		if(dp.getProfileid() >= 0) {
			logger.info("Saving an existing dataprofile");
			sf.getCurrentSession().update(dp);	//ideally this should be merge, but update work currently, so I won't change it
		}
		else {
			logger.info("creating a new dataprofile");
			
			//we need to attach it to the collection
			Collection c = (Collection)sf.getCurrentSession().get(Collection.class, collectionid);
			c.getProfiles().add(dp);

			//there used to be intermitant problems here caused by striping out the profileids above.
			sf.getCurrentSession().save(dp);
			
		}
	}

	@Transactional
	public void saveProfile(Profile dp, int contributorid, boolean production) {
		//how about we load up a copy of the dp from the database and try changing it
		if(dp.getProfileid() != null)
		{
			logger.info("merging profile into database");
			sf.getCurrentSession().merge(dp);
			
			sf.getCurrentSession().flush();

		} else
		{
			logger.info("saving new profile, profileid=" + dp.getProfileid());
			
			sf.getCurrentSession().save(dp);
			
			sf.getCurrentSession().flush();
			
			//now attach it to the contributor
			Contributor c = (Contributor)sf.getCurrentSession().get(Contributor.class, contributorid);
			if(production)
				c.setProduction(dp);
			else
				c.setTest(dp);
		}

		logger.info("save complete");
	}

	@Transactional
	public Contributor AttachDefaultProfile(int profileid, Contributor cf) 
	{
		Contributor c = (Contributor)sf.getCurrentSession().get(Contributor.class, cf.getContributorid());
		
		
		if(profileid > 0) {
			Profile p = (Profile)sf.getCurrentSession().get(Profile.class, profileid);
			
			logger.info("copying current collection profile def profileid=" + profileid);
			c.setProduction(clonedp(p));
			c.setTest(clonedp(p));
		} else {
			logger.info("creating new empty profiles to fill contributor's profiles");
			//create a new empty profile
			Profile prod = new Profile();
			Profile test = new Profile();
			c.setProduction(prod);
			c.setTest(test);
			
			sf.getCurrentSession().save(prod);
			sf.getCurrentSession().save(test);
		}
			
		cf.setProduction(c.getProduction());
		cf.setTest(c.getTest());
		return cf;
	}
	
	
	@Transactional
	public Contributor findCloneAndAttachDefaultProfile(Contributor cf)
	{
		Contributor c = (Contributor)sf.getCurrentSession().get(Contributor.class, cf.getContributorid());
		Profile def = null;
		int numprofilesofthistype = 0;
		for(Profile dp : c.getCollection().getProfiles())
		{
			if(dp.getType() == c.getHtype())
			{
				def = dp;
				numprofilesofthistype++;
			}
		}
		
		logger.info("def= " + def + " num profiles of this type=" + numprofilesofthistype);
		
		if(def != null && numprofilesofthistype == 1)
		{
			logger.info("copying current collection profile def profileid=" + def.getProfileid());
			c.setProduction(clonedp(def));
			c.setTest(clonedp(def));
			
		} else if (def == null)
		{
			logger.info("creating new empty profiles to fill contributor's profiles");
			//create a new empty profile
			Profile prod = new Profile();
			Profile test = new Profile();
			c.setProduction(prod);
			c.setTest(test);
			
			sf.getCurrentSession().save(prod);
			sf.getCurrentSession().save(test);
		} else {
			logger.info("detected multiple possible parent profiles, not adding any of them as the default");
		}
		
		cf.setProduction(c.getProduction());
		cf.setTest(c.getTest());
		return cf;
	}

	private Profile clonedp(Profile def) {
		
		Profile dp = new Profile();

		dp.setType(def.getType());			
		sf.getCurrentSession().save(dp);
		
		for(ProfileStep oldps : def.getProfilesteps())
		{
			//do not copy disabled steps
			if(oldps.getEnabled() == ProfileStep.DISABLED)
				continue;
			
			ProfileStep ps = new ProfileStep();
			ps.setProfile(dp);
			ps.setPosition(oldps.getPosition());
			//ps.setIsreadonly(oldps.getIsreadonly());
			ps.setEnabled(ProfileStep.ENABLED);	//don't copy its enabled state over
			ps.setRestriction(oldps.getRestriction());
			ps.setDescription(oldps.getDescription());
			ps.setStep((Step)sf.getCurrentSession().get(Step.class, oldps.getStep().getStepid()));
			
			dp.getProfilesteps().add(ps);
			
			for(ProfileStepParameter oldp : oldps.getParameters())
			{
				ProfileStepParameter p = new ProfileStepParameter();
				p.setValue(oldp.getValue());
				p.setPis((ParameterInformation)sf.getCurrentSession().get(ParameterInformation.class, oldp.getPis().getPiid()));
				p.setPss(ps);
				p.setGrouplistindex(oldp.getGrouplistindex());
				ps.getParameters().add(p);
			}
		}
		return dp;
	}

	@Transactional
	public void deleteProfile(int profileid, int collectionid) {
		Profile dp = null;
		//we need to do two things
		
		//nullify the profileid in all harvests that reference this profile					
		sf.getCurrentSession().createSQLQuery(
    			"UPDATE harvest SET profileid = null WHERE profileid = " + profileid)
    			.executeUpdate();
    	
		// delete the profile's tree, including the profile it's self
		//we do this to make sure that the association table joiing collection/profile is respected
		Collection col = (Collection)sf.getCurrentSession().get(Collection.class, collectionid);
		for(java.util.Iterator<Profile> itor = col.getProfiles().iterator(); itor.hasNext();) {
			Profile ip = itor.next();
			if(ip.getProfileid() == profileid) {
				dp = ip;
				itor.remove();
			}					
		}	
			
		if(dp == null) {
			logger.error("attempted to delete a non-existant profile!");
		} else {
			sf.getCurrentSession().delete(dp);
		}
	}
	
	@Transactional
	public Profile copyProfile(Integer profileid, int contributorid, boolean overproduction) {
		Profile dp = (Profile) sf.getCurrentSession().get(Profile.class, profileid);
		Profile newprofile = clonedp(dp);
		
		Contributor c = (Contributor) sf.getCurrentSession().get(Contributor.class, contributorid);
		if(overproduction)
			c.setProduction(newprofile);
		else
			c.setTest(newprofile);
		
		sf.getCurrentSession().flush();
		return newprofile;
	}

	@Transactional
	public void deleteProfile(int profileid) {
		Profile dp = (Profile) sf.getCurrentSession().get(Profile.class, profileid);
		if(dp != null)
		{
			//we need to do two things
			
			//nullify the profileid in all harvests that reference this profile					
			sf.getCurrentSession().createSQLQuery(
        			"UPDATE harvest SET profileid = null WHERE profileid = " + profileid)
        			.executeUpdate();
        	
			// delete the profile's tree, including the profile it's self
			sf.getCurrentSession().delete(dp);
		}
		else
		{
			logger.error("attempted to delete a non-existant profile!");
		}		
	}
	
	
	
}
