package harvester.client.service;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import harvester.client.connconfig.actions.LoadStepActions;
import harvester.client.data.dao.DAOFactory;
import harvester.client.util.EscapeHTML;
import harvester.data.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

@Service
public class CollectionService {
	
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;
	private Map<Integer, LoadStepActions> stepactions;
    
    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	@Required
	public void setStepactions(Map<Integer, LoadStepActions> stepactions) {
		this.stepactions = stepactions;
	}
    
    /**
     * Get the available output stages that the user might want to select from
     */
    public List<ProfileStep> getOutputStages() {
		return  daofactory.getCollectionDAO().getPossibleOutputStages();
    }
    
    /**
     * A list of all collections in the db with names and descriptions escaped.
     */
    public List<Collection> getCollectionListEscaped() {
    	
    	List<Collection> collections = daofactory.getCollectionDAO().getAllCollections();
        
        //various escaping needs to be done.
        for(Collection c : collections)
        {
        	c.setName(EscapeHTML.forHTML(c.getName()));
        	c.setDescription(EscapeHTML.forHTML(c.getDescription()));
        }
        return collections;
    }
    
    /**
     * build a model in the form of a map for the collection by adding escaped versions of the fields into the model
     * and adding information the model needs about contributors(how many are there?) and contacts(are there any?)
     * @param collectionid
     * @return A model suitable for use by spring
     */
    public Map<String, Object> getCollectionModel(int collectionid)
    {
    	Collection c = daofactory.getCollectionDAO().getCollectionAndDependents(collectionid);
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("now", new Date());
        
        model.put("collectionnameescaped", EscapeHTML.forHTML(c.getName()));
        model.put("collectiondescriptionescaped", EscapeHTML.forHTML(c.getDescription()));
        model.put("collectionuserguideescaped", EscapeHTML.forHTML(c.getUserguide()));
        
        int numcontributors = daofactory.getCollectionDAO().getContributorCount(c.getCollectionid());
        model.put("numcontributors", numcontributors);
        
        model.put("collection", c);
        Set<CollectionContact> contacts = c.getContacts();
        model.put("contacts", contacts);
        if(contacts == null || contacts.size() > 0)
        	model.put("nocontacts", false);
        else
        	model.put("nocontacts", true);
        
        ProfileStep outputstage = c.getLoadstage();
        if(outputstage != null)
        {
	        model.put("outputstage", outputstage);
        }
        
        return model;
    }

    /**
     * Build a new collection object
     */
	public Collection getNewCollection() {			
		return new Collection();	
	}
    
	/**
	 * Given a map holding submitted form information about contacts, this method extracts this information into 
	 * a set of collection contact objects.
	 * @param indexMax This is the number after which we stop searching for contacts. Should be greater(but not much greater) 
	 * then the largest contacts index. given by "biggestcontact" in the form usually(generated with javascript).
	 * @param contactMap map of submitted form parameters
	 * @param c a collection object for the contacts to reference. without which persisting the contacts doesn't work
	 * @return set of contacts
	 */
	public Set<CollectionContact> extractContacts(int indexMax, Map<String, String> contactMap, Collection c) {
		
		logger.info("extracting contacts, indexMax = " + indexMax);
		
		Set<CollectionContact> contacts = new HashSet<CollectionContact>();
		
		//our contacts indexes start at 1
		for(int i=1; i <= indexMax; i++) {
			String cname = contactMap.get("name" + i);
			if(cname == null)
				continue;
			if(cname == "")	{ //if they don't put anything in name, skip it
				logger.info("Empty contact name");
				continue;
			}
			String title = contactMap.get("title" + i);
			String email = contactMap.get("email" + i);
			String phone = contactMap.get("phone" + i);
			String notes = contactMap.get("notes" + i);
			String type = contactMap.get("type" + i);
			String contactid = null;
			contactid = contactMap.get("contactid" + i);

			logger.info("cname=" + cname);
			logger.info("contactid=" + contactid);

			//create a new contract object for this contact
			CollectionContact con = new CollectionContact();				
			con.setName(cname);
			con.setJobtitle(title);
			con.setEmail(email);
			con.setPhone(phone);
			con.setNote(notes);
			con.setType(Integer.valueOf(type));
			con.setCollection(c);
			if(contactid != null && contactid != "")
				con.setContactid(Integer.valueOf(contactid));
			else con.setContactid(-1);
			
			contacts.add(con);
			logger.info("contact added");
		}
		return contacts;
	}
	
	/**
	 * Get the collection corresponding to the given collectionid from the database.
	 * If the collectionid is null we instead create a new collection object that can be
	 * persisted later using hibernate.
	 * @param collectionid possibly null collection's id( negative numbers create a new collection as well)
	 * @return collection object
	 */
	public Collection getCollection(Integer collectionid) {
		Collection c;
		if(collectionid == null || collectionid <= 0) 
			 c = new Collection();
		else
			 c = daofactory.getCollectionDAO().getCollection(collectionid);
		
		return c;
	}
	
	/**
	 * Apply the modifications to the collection object, including the given fields, which should be 
	 * exactly as the collection's properties.
	 * @param c collection object that hibernate can persist
	 * @param description 
	 * @param userguide
	 * @param name
	 * @param contacts
	 * @param outputstage
	 */
	public void ModifyCollection(Collection c, String description, 
			String userguide, String name, Set<CollectionContact> contacts,
			Integer outputstage) {
		
		c.setDescription(description);
		c.setUserguide(userguide);
		c.setName(name);
		c.setContacts(contacts);

		c = daofactory.getCollectionDAO().saveOrUpdateCollection(c, outputstage);
	}
	
	public void deleteProductionRecordsForContributor(int collectionid, int contributorid) {
		
		Collection col = daofactory.getCollectionDAO().getCollection(collectionid);
		if(col.getLoadstage() != null) {
			logger.info("deleting production records from global store");
			Integer stepid = col.getLoadstage().getStep().getStepid();
			LoadStepActions loadaction = stepactions.get(stepid);
			if( loadaction != null) {
				Contributor con = daofactory.getContributorDAO().getContributor(contributorid);
				loadaction.deleteProductionRecords(contributorid, con.getName());
			} else {
				logger.info("no class configured to perform the deletion on the client");
			}
		}
		//even if we have no load step, make sure totalrecords is 0
		daofactory.getContributorDAO().setRecordCount(contributorid, 0);
	}
	
	public void updateCollectionSize(int collectionid) {
		
		Collection col = daofactory.getCollectionDAO().getCollection(collectionid);
		Integer size = null;
		
		if(col.getLoadstage() != null) {
			Integer stepid = col.getLoadstage().getStep().getStepid();
			LoadStepActions loadaction = stepactions.get(stepid);
			
			if( loadaction != null)
				size = loadaction.getCollectionSize();		
		}
		
		logger.info("got size '" + size + "' saving...");
		col.setSize(size);	//size can be null
		daofactory.getCollectionDAO().modifyCollection(col);	

	}
	
	public HashMap<String, Contributor> getScheduledContributors(int collectionid) {
		Collection col = daofactory.getCollectionDAO().getCollectionContributorsAndLastHarvests(collectionid);		
		Set<Contributor> contributors = col.getContributors();
		
		HashMap<String, Contributor> cons = new HashMap<String, Contributor>();
		
		for(Contributor con : contributors) {
			//Does this contributor have a schedule?
			if(con.getType() != 0 && con.getIsscheduled() != 0) {
				cons.put(String.valueOf(con.getContributorid()), con);
			}
		}
		
		return cons;
	}
	
	public List<Harvest> getRunningHarvests(int collectionid) {
		return daofactory.getCollectionDAO().getRunningHarvests(collectionid);
	}
	
}
