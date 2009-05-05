package harvester.client.profileconfig;

import harvester.client.data.dao.DAOFactory;
import harvester.client.util.EscapeHTML;
import harvester.data.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.request.WebRequest;

/**
 * Transparently maintains the session/state information for a profile being edited.
 * Should be called first thing in any class that is in the profile wizard. reads various 
 * parameters from the request object and provides access to them in a nicer way.
 * We also provide a centralized way of navigating between pages while maintaining the session
 * by providing url and form input stuff so that a page redirect will be guarenteed to be done
 * correctly.
 * 
 * Also provides a wrapper around the session scoped bean SessionProfileHandler instance that 
 * needs to be used to persist things in the session.
 * 
 * There should always be a collection or contributor, so we don't need to track that here.
 * 
 * To maintain session state, within redirects the page redirect url should be retrieved from
 * the methods this class exposes, which can the needed parameters already.
 * We even include in the generated model a string consisting of input tags to be used in 
 * html forms to make sure the needed parameters are in the posted form.
 * @author adefazio
 *
 */
public class ProfileSession {

    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
    
    private SessionProfileHandler sessionprofilehandler;

    @Required
	public void setSessionprofilehandler(SessionProfileHandler sessionprofilehandler) {
		this.sessionprofilehandler = sessionprofilehandler;
	}
	
    protected final Log logger = LogFactory.getLog(getClass());
	
    private Map<String, Object> model = new HashMap<String, Object>();
    private Collection col;
    private Contributor con;
    private boolean colview = false;
    private Profile p;
    private String returnurl;
    private String urlpostfix;
    private Integer profileEnvironment;

	private String position;
    
    /**
     * Effectively a constructor for this class. pulls its information from the passed map
     * which is expected to be much like a request's parameter map.
     * Always expects a profileid, and one and only one of (contributorid, collectionid). 
     * An exception is thrown if the map doesn't contain these. The only other thing that
     * that is extracted from the map is the "position" if it is in there, it will be passed
     * around in internal redirects to all pages except editprofile. It will never be included
     * in urls given to the model, but is in the form input tags we include in the model.
     * If the passed profileid is not already in the session, it wil NOT be added, but this object
     * will still be created referencing that profile if it exists in the database.
     * @param pmap	hashmap in the style of a request's parameter map
     * @throws Exception
     */
    public void initProfileSession(Map pmap) throws Exception {
    	logger.info("Initilising the profile session object");
		
		//print out the session contents for debugging
		logger.info(sessionprofilehandler.toString());

		int profileid = Integer.valueOf((String)pmap.get("profileid"));
		
		String scollectionid = (String)pmap.get("collectionid");
		String scontributorid = (String)pmap.get("contributorid");
		
		if(scollectionid != null && scontributorid != null)
			throw new Exception("can't handle both contributor and collection ids being passed in");		
		
		if(scollectionid != null) {
			int collectionid = Integer.valueOf(scollectionid);
			
			//retrieve the collection from the database
			col = daofactory.getCollectionDAO().getCollection(collectionid);			
			//add collectionname, collectionid to map
			colview = true;
			model.put("colview", true);	//signifies that this is a collection level view
			returnurl = "ViewProcessingProfiles.htm?collectionid=" + collectionid;
			model.put("returnurl", returnurl);
			
			p = sessionprofilehandler.getCollectionProfile(profileid, collectionid);
			
			//The only times when a profile won't be in the session is when they 'jump' to a page in the wizard
			//without going from the start page of the wizard. We will handle this as if they did go to the start.
			//Basically we just add the relevant profile to the session and continue on normally.
			if( p == null) {
				if(profileid == -1) {
					p = new Profile();
					p.setProfileid(-1);	
				} else {
					p = daofactory.getProfileDAO().getProfile(profileid);
				}
				addCollectionProfile(p, collectionid);	//adds to session
			}
			
			//if they changed the profile name or profiletype, we should persist that
			//String profiletype = (String)pmap.get("profiletype");
			//String profilename = (String)pmap.get("profilename");
			//if(profilename != null) p.setName(profilename);
			//if(profiletype != null) p.setType(Integer.valueOf(profiletype));
						
			model.put("profilename", p.getName());
			
		} else {	//contributor level view
			int contributorid = Integer.valueOf(scontributorid);
			
			con = daofactory.getContributorDAO().getContributorCollectionAndDataprofile(contributorid);
			col = con.getCollection();
			model.put("contributorname", con.getName());
			model.put("contributorid", con.getContributorid());
			model.put("contributortype", con.getType());
			returnurl = "ViewProcessingSteps.htm?contributorid=" + con.getContributorid();
			model.put("returnurl", returnurl);
			
			p = sessionprofilehandler.getContributorProfile(profileid, contributorid);
			//see the comment above for null profiles at collection level
			if(p == null) {	
				logger.info("profile not in session, retrieving from database");
				p = daofactory.getProfileDAO().getProfile(profileid);
				addContributorProfile(p, contributorid);	//adds to session
			}
			if(con.getProduction().getProfileid().equals(p.getProfileid())) {
				profileEnvironment = Profile.PRODUCTION_PROFILE;
				model.put("EnvironmentName", "Production Environment");
			} else {
				//logger.info("prod id=" + con.getProduction().getProfileid() + " this id=" + p.getProfileid());
				profileEnvironment = Profile.TEST_PROFILE;
				model.put("EnvironmentName", "Test Environment");
				model.put("test", true);
				
			}
		}
		
		//if other requests are made under this wizard thingy, we need them to also pass around these parameters
		//so we will build the urls here so that we do it right

		//if this is in one of the edit step type pages, we should pass around the position as well
		position = (String)pmap.get("position");
		if(position != null) {
			logger.info("got position");
			model.put("position", position);
		}

		model.put("profiletype", p.getType());
		model.put("profile", ProfileConfigUtil.getProfileView(p));

		urlpostfix = getSessionStateUrlPostfix();
		
		model.put("collectionname", col.getName());
		model.put("collectionid", col.getCollectionid());
		model.put("sessionStateHTML", getSessionStateHTML());
		//model.put("modifyUrl", EscapeHTML.forHTML(getModifyUrlNoPosition() ));
		//model.put("urlpostfix", EscapeHTML.forHTML(urlpostfix));
		model.put("addStepUrl", EscapeHTML.forHTML(getAddNewUrlNoPosition()));
		model.put("editStepUrl",EscapeHTML.forHTML(getEditStepUrlNoPosition()));
		model.put("editProfileUrl", EscapeHTML.forHTML(getEditProfileUrl()));
		
		//modifying stuff pages
		model.put("changeProfileStepPositionUrl", EscapeHTML.forHTML("ChangeProfileStepPosition.htm" + urlpostfix));
		model.put("deleteCollectionProfile",  EscapeHTML.forHTML("DeleteCollectionProfile.htm" + urlpostfix));
		model.put("deleteProfileStepUrl", EscapeHTML.forHTML("DeleteProfileStep.htm" + urlpostfix));
		model.put("goBackInStepWizardUrl",EscapeHTML.forHTML( "GoBackInStepWizard.htm" + urlpostfix));
		model.put("saveProfileUrl", EscapeHTML.forHTML("SaveProfile.htm" + urlpostfix));
		model.put("preserveProfileEditUrl",  EscapeHTML.forHTML("PreserveProfileEdit.htm" + urlpostfix));
		model.put("addFromCollectionPage1Url",  EscapeHTML.forHTML("AddFromCollectionPage1.htm" + urlpostfix));
	    model.put("copyStepFromCollectionUrl", EscapeHTML.forHTML("CopyStepFromCollection.htm" + urlpostfix));
	    model.put("copyProfileFromCollectionUrl", EscapeHTML.forHTML("CopyProfileFromCollection.htm" + urlpostfix));	    
		
		//the restriction types for profilesteps
		model.put("None", ProfileStep.NORMAL);
		model.put("Mandatory", ProfileStep.MANDATORY);
		model.put("Locked", ProfileStep.LOCKED);
		
    }
    
    /**
     * See initProfileSession(Map pmap) for the description of functionality, this method just generates a map
     * from the request given and calls that method with it.
     * @param request
     * @throws Exception
     */
	public void initProfileSession(HttpServletRequest request) throws Exception {	
		
		// start by printing it all out of debugging
		for(Object key : request.getParameterMap().keySet() ) {
			logger.info("| key: " + key + " value: " + request.getParameter((String) key));
		}
		
		Map pmap = new HashMap();
		Set names = request.getParameterMap().keySet();
		for(Object name : names)
		{
			logger.info(name + " = " + request.getParameter((String)name));
			//add the parameters from the request to this map
			pmap.put(name, request.getParameter((String) name));
		}		
		initProfileSession(pmap);
	}
	//TODO: we want to remove the use of the http version in favor of only using the generic webrequest version.
	public void initProfileSession(WebRequest request) throws Exception {	
		
		// start by printing it all out of debugging
		for(Object key : request.getParameterMap().keySet() ) {
			logger.info("| key: " + key + " value: " + request.getParameter((String) key));
		}
		
		Map pmap = new HashMap();
		Set names = request.getParameterMap().keySet();
		for(Object name : names)
		{
			logger.info(name + " = " + request.getParameter((String)name));
			//add the parameters from the request to this map
			pmap.put(name, request.getParameter((String) name));
		}		
		initProfileSession(pmap);
	}
	
	/**
	 * retrives the environment of the wrapped profile. This will be either PRODUCTION or TEST, represented by an integer
	 * That should be compared against the enumeration in the Profile class.
	 * @return	profile enumeration integer
	 * @throws Exception
	 */
	public int getProfileEnvironment() throws Exception {
		if(profileEnvironment == null)
			throw new Exception("don't have an environment for a collection level profile");
		else
			return profileEnvironment;
	}
	
	/**
	 * Returns a string of html input tags that will keep the session active if using a form post.
	 */
	private String getSessionStateHTML() {
		StringBuilder html = new StringBuilder();
		html.append("<input type=\"text\" style=\"display:none\"  name=\"profileid\" value=\"" + p.getProfileid() + "\" />\n");
		if(colview)
			html.append("<input type=\"text\" style=\"display:none\"  name=\"collectionid\" value=\"" + col.getCollectionid() + "\" />\n");			
		else
			html.append("<input type=\"text\" style=\"display:none\"  name=\"contributorid\" value=\"" + con.getContributorid() + "\" />\n");
		
		if(position != null)
			html.append("<input type=\"text\" style=\"display:none\"  name=\"position\" value=\"" + position + "\" />\n");
		
		return html.toString();
	}
	
	/**
	 * Returns the url query parameters that should be appended to a url to maintain the session.
	 * Starts with a '?' and further query parameters can be appended in the obvious way
	 * @return
	 */
	public String getSessionStateUrlPostfix() {
		StringBuilder urlsb = new StringBuilder();
		urlsb.append("?profileid=" + p.getProfileid());
		if(colview)
			urlsb.append("&collectionid=" + col.getCollectionid());
		else
			urlsb.append("&contributorid=" + con.getContributorid());

		return urlsb.toString();
	}
	
	/**
	 * Is this a collection level profile wrapped( as opposed to a contributor level one)
	 */
	public boolean isCollectionLevel() {
		return colview;
	}
	
	public Map<String, Object> getModel() {
		return model;
	}
	
	public Profile getProfile() {
		return p;
	}
	
	/**
	 * returns the id of the owner of the wrapped profile. Will be either a contributorid or a 
	 * collection id.
	 */
	public int getOwnerid() {
		return colview ? col.getCollectionid() : con.getContributorid();
	}

	public int getProfileid() {
		return p.getProfileid();
	}
	
	/**
	 * The url that should be used for escaping from the profile editing, back to the profile view page.
	 * @return
	 */
	public String getReturnurl() {
		return returnurl;
	}
	
	public void removeProfileFromSession() {
		sessionprofilehandler.remove(p);
	}

	private String positionurlpostfix() {
		return position == null ? "" : "&position=" + position;
	}
	
	public String getEditStepUrl() {
		return getEditStepUrlNoPosition() + positionurlpostfix(); 
	}
	private String getEditStepUrlNoPosition() {
		return "EditProcessingStep.htm" + urlpostfix; 
	}
	
	public String getModifyUrl() {
		return getModifyUrlNoPosition() + positionurlpostfix(); 
	}
	private String getModifyUrlNoPosition() {
		return "ModifyProcessingSteps.htm" + urlpostfix;
	}
	public String getAddNewUrl() {
		return getAddNewUrlNoPosition() + positionurlpostfix();  
	}
	private String getAddNewUrlNoPosition() {
		return "AddNewStep.htm" + urlpostfix;
	}
	public String getEditProfileUrl() {
		return "EditProcessingSteps.htm" + urlpostfix;
	}

	public String getAddFromCollectionPageTwoUrl() {
		return "AddFromCollectionPage2.htm" + urlpostfix;
	}
	/**
	 * Adds the given profile to the session.
	 */
	public void addContributorProfile(Profile dp, int contributorid) {
		logger.info("adding contributor profile profileid=" + dp.getProfileid() + " contributorid=" + contributorid);
		sessionprofilehandler.addContributorProfile(dp, contributorid);
	}

	/**
	 * Adds the given profile to the session.
	 */
	public void addCollectionProfile(Profile dp, int collectionid) {
		logger.info("adding collection profile profileid=" + dp.getProfileid() + " collectionrid=" + collectionid);
		sessionprofilehandler.addCollectionProfile(dp, collectionid);		
	}
	
	/**
	 * retrieve the position that was passed into the session, casted to an int
	 * throws exception if there is no position
	 */
	public int getPosition() {
		return Integer.valueOf(position);
	}
	
	public Contributor getContributor() {
		return con;
	}
	
    public Profile getTmpProfile() {
		return sessionprofilehandler.getTmpProfiles().get(p.getProfileid());
	}

	public void setTmpProfile(Profile tmpProfile) {
		sessionprofilehandler.getTmpProfiles().put(p.getProfileid(), tmpProfile);
	}

	/**
	 * Basically apply the changes made in tmp to the main session profile by replacing it.
	 */
	public void copyTmpToMain() {
		sessionprofilehandler.addContributorProfile(getTmpProfile(), con.getContributorid());
		
	}

	
}
