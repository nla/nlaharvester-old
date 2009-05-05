package harvester.client.service;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import harvester.client.connconfig.ConnectionSettings;
import harvester.client.connconfig.InputPluginConfigurer;
import harvester.client.connconfig.PUtil;
import harvester.client.connconfig.SessionConnectionHandler;
import harvester.client.data.dao.DAOFactory;
import harvester.client.util.EscapeHTML;
import harvester.client.util.KeyValue;
import harvester.data.Collection;
import harvester.data.CollectionContact;
import harvester.data.ContactSelections;
import harvester.data.Contributor;
import harvester.data.ContributorContact;
import harvester.data.Step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContributorService {
	
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
    
    private SessionConnectionHandler sessionconnectionhandler;
    private InputPluginConfigurer inputpluginconfigurer;

    @Autowired
	public void setSessionconnectionhandler(
			SessionConnectionHandler sessionconnectionhandler) {
		this.sessionconnectionhandler = sessionconnectionhandler;
	}

	@Autowired
	public void setInputpluginconfigurer(InputPluginConfigurer inputpluginconfigurer) {
		this.inputpluginconfigurer = inputpluginconfigurer;
	}
    
	/**
	 * Retrieve the collection specified and all of its contributors from the session and perform
	 * the required escaping for the contributors to be displayed in the views.
	 */
    public Collection getEscapedContributors(int collectionid) {
    	
    	Collection col = daofactory.getCollectionDAO().getCollectionContributorsAndLastHarvests(collectionid);
 		
 		for(Contributor c : col.getContributors())
 		{
 			c.setName(EscapeHTML.forHTML(c.getName()));
 			c.setDescription(EscapeHTML.forHTML(c.getDescription()));
 			c.setPlatform(EscapeHTML.forHTML(c.getPlatform()));
 		}
 		return col;
    }
    
    /**
     * Get a single contributor from the database with its string fields escaped
     * @param contributorid
     * @return
     */
    public Contributor getEscapedContributor(int contributorid) {
		Contributor c = daofactory.getContributorDAO().getContributorCollectionContactsAndLastHarvest(contributorid);
		
		c.setName(EscapeHTML.forHTML(c.getName()));
		c.setDescription((EscapeHTML.forHTML(c.getDescription())));
		c.setPlatform(EscapeHTML.forHTML(c.getPlatform()));
		
		return c;
    }

    /**
     * Given the collection specified, we add a new contributor onto it in the session(this creates a new connection settings object)
     * with contributor id -1 that can be persisted later if needed. We mark this as a 'new' contributor as well.
     * @param collectionid 
     */
	public void addNewContributorToSession(int collectionid) {
		
		Contributor c = new Contributor();
		c.setContributorid(-1);
		c.setDateadded(new Date());
		c.setCollection(daofactory.getCollectionDAO().getCollectionAndContacts(collectionid));
		
		//save contributor in the session as well
		sessionconnectionhandler.getSettings().put(c.getContributorid(), new ConnectionSettings());
		ConnectionSettings cs = sessionconnectionhandler.getConnectionSetting(c.getContributorid());
		cs.setC(c);
		cs.setNewContributor(true);
				
		c.setContacts(new HashSet<ContributorContact>());
		
	}
    
	/**
	 * Adds a new connection settings object to the session and attaches to it the given contributor
	 * pulled from the database. the contributor must exist. This also performs escaping on the 
	 * contributor.
	 * @param contributorid
	 */
	public ConnectionSettings addExistingContributorToSession(int contributorid) {
		
		//we need the connection settings since this is used for both editing connection settings and contributors
		Contributor c = daofactory.getContributorDAO().getContributorAndConnectionSettings(contributorid);
		
		c.setName(EscapeHTML.forHTML(c.getName()));
		c.setDescription(EscapeHTML.forHTML(c.getDescription()));
		c.setPlatform(EscapeHTML.forHTML(c.getPlatform()));
		
		sessionconnectionhandler.getSettings().put(c.getContributorid(), new ConnectionSettings());
		ConnectionSettings cs = sessionconnectionhandler.getConnectionSetting(c.getContributorid());
		cs.setC(c);
		return cs;
	}
	
	public Contributor getContributorFromSession(int contributorid) {
		 return sessionconnectionhandler.getConnectionSetting(contributorid).getC();
	}
	
	/**
	 * Gets the harvest type that the user selected earlier, from the session. 
	 * if no harvest type has been selected then returns null
	 * @param contributorid
	 */
	public Integer getSelectedHarvestType(int contributorid) {
		Step s = sessionconnectionhandler.getConnectionSetting(contributorid).getStep();
		return s == null ? null : s.getStepid(); 
	}

	/**
	 * Get a list of the possible harvest types that the user could use for contributors underneath
	 * the given collection.
	 * @param col a collection object
	 * @return A list of input stage tuples, key is the name, value is the stepid
	 */
	public List<KeyValue> getHarvestTypes(Collection col) {
		return PUtil.getPossibleInputStages(daofactory, col, inputpluginconfigurer);
	}
	
	public void fillMissingCollectionContacts(Contributor c) {
		
		//basically, we need to create an extra contactselection for every collection
		//contact that doesn't yet have one.
		//So we loop over the collection contacts to do this.
		for(CollectionContact colcontact : c.getCollection().getContacts()) {
			
			boolean hasSelection = false;
			for(ContactSelections consel : c.getContactselections()) {
				if(consel.getContact().getContactid() == colcontact.getContactid())
					hasSelection = true;
			}
			
			//does it have a selection yet?
			if(!hasSelection) {
				//create a new selection object
				ContactSelections consel = new ContactSelections();
				consel.setContact(colcontact);
				consel.setContributor(c);
				consel.setSelectionid(-1);
				
				//add to contributors selection set
				c.getContactselections().add(consel);
			}
		}
	}
	
	/**
	 * Persist the specified contributor to the database(the contributor is retrieved from the session
	 * using the given contributorid). If it is a new contributor, we also attach the profile the user
	 * selected earlier to the contributor.
	 * @param contributorid	must correspond to a contributor in the session
	 * @param isNew
	 */
	public void SaveContributor(int contributorid, boolean isNew) {
		logger.info("saving the contributor object stored in the session into the database");

		// get the contributor from the session
		Contributor c = sessionconnectionhandler.getConnectionSetting(contributorid).getC();
		//now save it
		daofactory.getContributorDAO().saveOrUpdateContributor(c);
		
		Integer profileid = sessionconnectionhandler.getConnectionSetting(contributorid).getSelectedProfile();
		
		//if our parent collection has a profile setup, and this is a new contributor, setup a default profile
		if(isNew && profileid != null) {
			logger.info("new contributor, calling dao to handle profile stuff");
			sessionconnectionhandler.getConnectionSetting(contributorid).setC(
					daofactory.getProfileDAO().AttachDefaultProfile(profileid, c));
		} else {
			logger.info("not a new contributor, so not copying any profiles");
		}
		
		//so that if they also use SaveConnectionSettings, there is a copy in the right spot
		sessionconnectionhandler.getSettings().put(c.getContributorid(), sessionconnectionhandler.getConnectionSetting(contributorid));
	}
	
	/**
	 * Persist the connection settings in the session for the given contributor. 
	 * Also calls a hibernate update on the contributor so that the htype of the contributor
	 * is persisted as well.
	 * @param contributorid	must correspond to a contributor in the session
	 */
	public void SaveConnectionSettings(int contributorid) {
		logger.info("Saving connection settings");
		
		//if things like the granularity have changed, we need to make sure we save that
		Contributor c = sessionconnectionhandler.getConnectionSetting(contributorid).getC();
		//now save it
		daofactory.getContributorDAO().updateContributor(c);
			
		//we have a list of key values in the session that should now be saved
		ConnectionSettings cs = sessionconnectionhandler.getConnectionSetting(contributorid);
		Map<Integer, String> parameters = cs.getFinalvalues();
		
		//TODO: look into moving some of this into this method from the dao
		daofactory.getContributorDAO().addNewHarvestStage(c.getContributorid(), cs.getStepid(), parameters);
		
		logger.info("connection setttings saved");
	}
	
	/**
	 * Extact from pmap a contact that has its fields suffixed with suffix. attaches
	 * to the contact the passed contributor so the contact can be easily persisted with
	 * hibernate, and adds the contact underneath the contributor. 
	 * @param pmap a map that normally contacts information directly posted from a form
	 * @param suffix a string that the fields will all have as a suffix
	 * @param c a contributor object
	 */
	private void getContact(Map<String, String> pmap, String suffix, Contributor c) {
		try {
			//handle basic error cases
			
			String cname = pmap.get("name" + suffix);
			if(cname == null) {
				logger.info("name is null");
				return;
			}
			if(cname == "")	//if they don't put anything in name, skip it
			{
				logger.info("Empty contact name");
				return;
			}				
			
			//the two types of contact are differenciated by the type field
			
			String title = pmap.get("jobtitle" + suffix);
			String email = pmap.get("email" + suffix);
			String phone = pmap.get("phone" + suffix);
			String type = pmap.get("type" + suffix);
			String businesstype = pmap.get("businesstype" + suffix);
			String record = pmap.get("record" + suffix);
			String harvest = pmap.get("harvest" + suffix);
			String failure = pmap.get("failure" + suffix);
			String success = pmap.get("success" + suffix);
			String contactid = pmap.get("contactid" + suffix);
			String selectionid = pmap.get("selection" + suffix);
			
			logger.info("cname=" + cname);
			logger.info("contactid=" + contactid);
			logger.info("type=" + type);
			
			int ibusinesstype = 0;	//default, should not ever be needed
			if(businesstype != null && !businesstype.equals("")) 
				ibusinesstype = Integer.valueOf(businesstype);

			
			//what type of contact is this?
			if(ibusinesstype == ContactSelections.FROM_OTHER) {		
				logger.info("creating contact from other");
				
				//create a new contact object for this contact
				ContributorContact con = new ContributorContact();	
				con.setContributor(c);
				con.setName(cname);
				con.setJobtitle(title);
				con.setEmail(email);
				con.setPhone(phone);
				if(businesstype != null && !businesstype.equals("")) 
					con.setBusinesstype(Integer.valueOf(businesstype));
				else
					con.setBusinesstype(0);	//we should always have a businesstype anyway
				if(record != null && record.equals("on")) con.setRecord(1); else con.setRecord(0);
				if(harvest != null && harvest.equals("on")) con.setHarvest(1); else con.setHarvest(0);
				if(failure != null && failure.equals("on")) con.setFailure(1); else con.setFailure(0);
				if(success != null && success.equals("on")) con.setSuccess(1); else con.setSuccess(0);
				
				con.setType(Integer.valueOf(type));
				if(contactid != null && !"".equals(contactid))
					con.setContactid(Integer.valueOf(contactid));
				else con.setContactid(-1);
			
				c.getContacts().add(con);
			} else {
				logger.info("creating contact form NLA, selectionid=" + selectionid);
				//this a selected collection contact
				CollectionContact colcon = new CollectionContact();
				colcon.setContactid(Integer.valueOf(contactid));
				colcon.setName(cname);
				colcon.setEmail(email);
				colcon.setJobtitle(title);
				colcon.setPhone(phone);
				colcon.setType(Integer.valueOf(type));
				ContactSelections sel = new ContactSelections();
				sel.setContributor(c);
				sel.setContact(colcon);
				sel.setSelectionid(Integer.valueOf(selectionid));
				
				if(record != null && record.equals("on")) sel.setRecord(1); else sel.setRecord(0);
				if(harvest != null && harvest.equals("on")) sel.setHarvest(1); else sel.setHarvest(0);
				if(failure != null && failure.equals("on")) sel.setFailure(1); else sel.setFailure(0);
				if(success != null && success.equals("on")) sel.setSuccess(1); else sel.setSuccess(0);
				
				c.getContactselections().add(sel);
			}
		} catch (Exception e)
		{
			logger.info("error while parsing contact: " + e.toString(), e);
		}
	}
	
	/**
	 * given the fields of the contributor passed, we update the session's contributor object.
	 * Also clears the contacts of the contributor, and if the contributor doesn't exist in 
	 * the session we add it in with a new connection settings object.
	 * @param contributorid
	 * @param name
	 * @param description
	 * @param platform
	 * @return updated contributor object
	 */
	public Contributor updateContributorInSession( int contributorid, String name, String description, String platform) {

		logger.info("|name=" + name + " |description=" + description + " |platform=" + platform);

		/////////////////////////////////////////////
		//we need to get the contributor object before we can extract the contact information
		if(sessionconnectionhandler.getConnectionSetting(contributorid) == null)
			sessionconnectionhandler.getSettings().put(contributorid, new ConnectionSettings());

		//now we fetch the current contributor
		Contributor c = sessionconnectionhandler.getConnectionSetting(contributorid).getC();
		
		//we ignore the current contents of the contacts sets, since we have all the information in the request anyway
		c.setContacts(new HashSet<ContributorContact>());
		c.setContactselections(new HashSet<ContactSelections>());	

		/////////////////////////////////////////////

		//update any fields that need updating
		c.setName(name.trim());
		c.setDescription(description.trim());
		c.setPlatform(platform.trim());
		
		return c;
	}
	
	/**
	 * Add all contacts with index less then biggestcontact in pmap onto the given contributor,
	 * @param c a contributor object
	 * @param biggestcontact the index at which we stop searching for contacts in the map(a max bascially)
	 * @param pmap a map that generally corresponds to the posted form contents of a contact edit page
	 */
	public void AddContacts(Contributor c, int biggestcontact, Map<String, String> pmap) {
		//our contacts indexes start at 1, and there are two sets of them
		logger.info("processing contact information");
		for(int i=1; i <= biggestcontact; i++)
		{				
			getContact(pmap, "A" + i, c);					
			getContact(pmap, "B" + i, c);
		}
	}
	
	/**
	 * Save the given harvesttype onto the contributor specified in the session.
	 * Extracts information about the step for that harvesttype and saves that also.
	 * @param contributorid 
	 * @param harvesttype the id for a step in the db.
	 */
	public void saveHarvestTypeInSession(int contributorid, int harvesttype) {
		//new contributors need to have the selected harvest type saved as well.
		logger.info("user selected harvesttype=" + harvesttype);
		ConnectionSettings cs = sessionconnectionhandler.getConnectionSetting(contributorid);
		Step s = daofactory.getStepDAO().getStep(harvesttype);
		cs.setStep(s);

		inputpluginconfigurer.setHtype(cs.getC(), s.getClassname());	//type as in a number representing OAI/webcrawl/other etc.
		
	}	
}
