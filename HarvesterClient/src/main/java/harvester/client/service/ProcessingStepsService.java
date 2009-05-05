package harvester.client.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import harvester.client.connconfig.ConnectionSettings;
import harvester.client.connconfig.StepParameterView;
import harvester.client.data.dao.DAOFactory;
import harvester.client.profileconfig.ICustomizedStep;
import harvester.client.profileconfig.ProfileConfigUtil;
import harvester.client.profileconfig.ProfileSession;
import harvester.client.profileconfig.Stage;
import harvester.client.profileconfig.StepPluginConfigurer;
import harvester.client.util.KeyValue;
import harvester.client.util.WebUtil;
import harvester.data.Contributor;
import harvester.data.Profile;
import harvester.data.ProfileStep;
import harvester.data.ProfileStepParameter;
import harvester.data.Step;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;

@Service
public class ProcessingStepsService {

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
	
    public Contributor getContributorWithDetailedProfileInformation(int contributorid) {
		return daofactory.getContributorDAO().getContributorCollectionAndDetailedDataprofile(contributorid);		
    }
    
    public Profile getProfileFromDb(int profileid) {
    	return daofactory.getProfileDAO().getProfile(profileid);
    }
    
    private StepPluginConfigurer steppluginconfigurer;

    @Autowired
	public void setSteppluginconfigurer(StepPluginConfigurer steppluginconfigurer) {
		this.steppluginconfigurer = steppluginconfigurer;
	}

	public ProfileStep addNewProfileStepInSession(ProfileSession profilesession, int stepid) {
		
		//stick new flag in the session
		Step s = daofactory.getStepDAO().getStep(Integer.valueOf(stepid), true, true);
		//create a new profilestep object
		logger.info("creating profilestep with stepid=" + stepid + " stepname=" + s.getName());
		ProfileStep stage = new ProfileStep();
		stage.setPosition(profilesession.getPosition());
		stage.setProfile(profilesession.getProfile());
		stage.setEnabled(ProfileStep.ENABLED);
		stage.setStep(s);
		//save it in the session under our profile
		profilesession.getProfile().getProfilesteps().add(stage);
		
		return stage;
	}
	
	public ProfileStep findProfileInSessionAndRefreshFolders(ProfileSession profilesession) throws Exception {
		
		ProfileStep stage = null;
		//if this step is already in the pipeline we loop through to get it
		for(ProfileStep ps : profilesession.getProfile().getProfilesteps())
			if(ps.getPosition() == profilesession.getPosition())
				stage = ps;
		
		//we need to update the step object attached to this stage as well
		//if the step is not in the session this will fail and throw an exception. Which is what we want to happen
		Step s = daofactory.getStepDAO().getStep(stage.getStep().getStepid(), true, true);
		stage.setStep(s);
		
		return stage;
	}

	public String getViewOrDefault(String defaultview, String classname) {
		String viewname = steppluginconfigurer.getAlternateviews().get(classname);		
		if(viewname == null)
			viewname = defaultview;
		logger.info("alternative view = " + viewname);
		return viewname;
	}

	public void applyCustomizedPreprocessing(String classname, Map<String, Object> model) {
		
		ICustomizedStep customized = steppluginconfigurer.getCustomizedSteps().get(classname);
		if(customized != null) {
			logger.info("using customized preprocessor on the model");
			customized.PreProcess(model);
		}
	}
	
	public Map<Integer,String> buildTypeMap() {
		Map<Integer,String> typemap = new HashMap<Integer, String>();
		typemap.put(Step.INPUT, "Input");
		typemap.put(Step.OUTPUT, "Output");
		typemap.put(Step.VALIDATOR, "Validator");
		typemap.put(Step.TRANSLATOR, "Translator");
		return typemap;
	}

	public List<Step> getAllStepsList() {
		return daofactory.getStepDAO().getAllSteps();
	}

	public void deleteCollectionProfile(int profileid, int collectionid) {
		logger.info("deleting profile #" + profileid);
		daofactory.getProfileDAO().deleteProfile(profileid, collectionid);
	}

	public void changeStagePosition(int position, String direction, Profile p) {
		//since we know the position, we can do this in one loop
		//this code relies on positions being unique, and not having any gaps
		if(direction.equals("down"))
		{
			//probably a good idea so check if this is a valid operation first
			if(p.getProfilesteps().size() == position)
			{
				logger.info("tried to move a stage above the top!!");					
			}
			else
			{
				//we basically swap the element above it in the list
				for(ProfileStep ps : p.getProfilesteps())
				{
					if(ps.getPosition() == position)
						ps.setPosition(ps.getPosition()+1);
					else if(ps.getPosition() == (position+1))
						ps.setPosition(ps.getPosition()-1);
				}
			}
		}
		else	//move up
		{
			//probably a good idea so check if this is a valid operation first
			if(position == 1)	//1 is the bottom element
			{
				logger.info("tried to move a stage below the bottom!!");					
			}
			else
			{
				//we basically swap the element below it in the list
				for(ProfileStep ps : p.getProfilesteps())
				{
					if(ps.getPosition() == position)
						ps.setPosition(ps.getPosition()-1);
					else if(ps.getPosition() == (position-1))
						ps.setPosition(ps.getPosition()+1);
				}
			}
		}
	}

	public void saveProfile(ProfileSession profilesession, String profilename, Integer profiletype) throws Exception {
    	Profile p = profilesession.getProfile();

		if(profilesession.isCollectionLevel())
		{
			p.setName(profilename);
			p.setType(profiletype);
			
			daofactory.getProfileDAO().saveCollectionProfile(p, profilesession.getOwnerid());
		} else {
			boolean production = ( profilesession.getProfileEnvironment() == Profile.PRODUCTION_PROFILE ) ;
			daofactory.getProfileDAO().saveProfile(p, profilesession.getOwnerid(), production);
		}
	}
	
	@SuppressWarnings("unchecked")
	public String preserveEditToSession(HttpServletRequest request, ProfileSession profilesession) throws Exception {
		
		logger.info("preserving the edit");
		
		//we need to copy the parameters from the request map to our own map
		///////////////////////////////////////////////////////////////////
		Map pmap = new HashMap();
		
		if (ServletFileUpload.isMultipartContent(request)) {
			  // Parse the HTTP request...
			logger.info("this is a multipart form response");
			
			ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
			List fileItemsList = servletFileUpload.parseRequest(request);
			
			for(Object ritem : fileItemsList) {
				FileItem item = (FileItem)ritem;
				
				if( item.isFormField()) {
					logger.info(item.getFieldName() + " = " + item.getString());						
					pmap.put(item.getFieldName(), item.getString());
				} else {
					logger.info("FILE: " + item.getFieldName() + " size=" + item.getSize() + " name=" + item.getName());
					pmap.put(item.getFieldName(), WebUtil.slurp(item.getInputStream()));	//converts to a string		
					pmap.put(item.getFieldName() + "name", item.getName());
				}
			}									
		} else {
		
			Set names = request.getParameterMap().keySet();
			for(Object name : names) {
				logger.info(name + " = " + request.getParameter((String)name));
				//add the parameters from the request to this map
				pmap.put(name, request.getParameter((String) name));
			}
		}
		/////////////////////////////////////////////////////////////////////
		
		profilesession.initProfileSession(pmap);
		
		int position = Integer.valueOf((String) pmap.get("position"));
		int newposition = Integer.valueOf((String) pmap.get("newposition"));			
		
		Profile p = profilesession.getProfile();
		//a bunch of parameters have been passed in through a form, keyed by piid
		//just pass the job onto profileutil
		///////////////////////////////////////////////////////////////////////
		
		//find our stage in the session
		ProfileStep stage = null;
		for(ProfileStep ps : p.getProfilesteps())
			if(ps.getPosition() == position)
				stage = ps;
		
		stage.setDescription((String) pmap.get("description"));
		
		//String readonly = (String)pmap.get("readonly");
		//stage.setRestriction(("on".equals(readonly) ? true : false );
		if(pmap.containsKey("restriction"))
			stage.setRestriction(Integer.valueOf((String)pmap.get("restriction")));
		
		if(stage.getStep() != null)
			logger.info("stage stepid = " + stage.getStep().getStepid() + "name=" + stage.getStep().getName());
		else 
			logger.info("no step attached to stage");
		
		
		if(stage != null && steppluginconfigurer != null && steppluginconfigurer.getCustomizedSteps() != null) {
			ICustomizedStep customized = steppluginconfigurer.getCustomizedSteps().get(stage.getStep().getClassname());
			if(customized != null) {
				pmap = customized.PostProcess(pmap);
				
				logger.info("request parameters after processing");
				for(Object name : pmap.keySet())
					logger.info(name + " = " + pmap.get(name));
			} else {
				logger.info("could not find a customized postprocessor");
			}
			
		} else {
			logger.error("unable to get map of customized steps!");
		}
		
		ProfileConfigUtil.updateparameters(stage, pmap);
		//move the step if they changed its position
		if(stage.getPosition() != newposition)
			ProfileConfigUtil.changePosition(p, stage, newposition);
		
		return profilesession.getEditProfileUrl();
	}
	
	/**
	 * Get a list of the owning collection's profiles suitable for building a drop down
	 * @param contributorid
	 * @return view of the profile.
	 */
	public StepParameterView buildViewOfProfileAsParameter(ProfileSession profilesession) {

		StepParameterView profilespv = new StepParameterView();
		profilespv.setName("Processing Profile:");
		profilespv.setId(StepParameterView.PROFILE_ID);	//profilenumber
		profilespv.setEditibility(1);	//means required
		LinkedList<KeyValue> profiles = daofactory.getCollectionDAO().getDefaultProfilesForCollection(
					profilesession.getContributor().getCollection().getCollectionid()
				  , profilesession.getContributor().getHtype());
		
		profilespv.setType(StepParameterView.DROP_DOWN);
			
		profilespv.setOptions(profiles);
		
		return profilespv;
	}
	
	/**
	 * Clone just the top level of the profile, linking each profilestep object to the old object.
	 * @param p
	 * @return new profile
	 */
	public Profile shallowCloneProfile(Profile p) {
		Profile np = new Profile();
		np.setDescription(p.getDescription());
		np.setName(p.getName());
		np.setProfileid(p.getProfileid());
		np.setProfilesteps(new HashSet<ProfileStep>(p.getProfilesteps()));
		np.setType(p.getType());
		return np;
	}

	/**
	 * append a copy of the given profile step to the passed profile, with all ids cleared out, suitable for
	 * saving as new in the db.
	 * @param copyStep
	 * @param profile
	 */
	public void copyProfileStepToEnd(ProfileStep copyStep, Profile profile) {

		//traverse the profile steps tree, clearing out the ids
		
		ProfileStep newStep = (ProfileStep) copyStep.clone();
		newStep.setPsid(null);
		
		for(ProfileStepParameter psp : newStep.getParameters()) {
			psp.setProfilestepparameterid(null);
		}
		
		//set it up as in the profile
		
		newStep.setPosition(profile.getProfilesteps().size() + 1 );	//last element has position size()
		newStep.setProfile(profile);
		profile.getProfilesteps().add(newStep);
		
	}

	public void persistMainChanges(ProfileSession profilesession, WebRequest request) {
		
		logger.info("persisting main changes");
		
		//set any enabled check boxes if we were passed any
		if("true".equals(request.getParameter("changeEnabledSteps"))) {
			logger.info("changing enabled steps");
			int i = 1;
			for(ProfileStep ps : profilesession.getProfile().getProfilesteps()) {
				String isEnabled = request.getParameter(String.valueOf(i) + ".enabled");
				if(isEnabled != null && isEnabled.equals("on"))
					ps.setEnabled(ProfileStep.ENABLED);
				else {
					logger.info("disabling position " + i);
					ps.setEnabled(ProfileStep.DISABLED);
				}
				i++;
			}
		} else {
			logger.info("not changing enabled steps");
		}
		
		//change the profile name and type if this is a collection level profile
		
		//if they changed the profile name or profiletype, we should persist that
		String profiletype = request.getParameter("profiletype");
		String profilename = request.getParameter("profilename");
		if(profilename != null) profilesession.getProfile().setName(profilename);
		if(profiletype != null) profilesession.getProfile().setType(Integer.valueOf(profiletype));
		
		
	}
	
	public boolean isProfileCopyable(List<Stage> profile) {	
        
		boolean copyable = true;
		for(Stage s : profile) {
			if(s.getRestriction() != ProfileStep.NORMAL && s.getEnabled() == ProfileStep.DISABLED) {
				copyable = false;
			}
		}
		
		return copyable;
	}
}
