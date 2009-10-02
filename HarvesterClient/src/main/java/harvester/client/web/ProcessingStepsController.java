package harvester.client.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import harvester.client.profileconfig.ICustomizedStep;
import harvester.client.profileconfig.ProfileConfigUtil;
import harvester.client.profileconfig.ProfileSession;
import harvester.client.profileconfig.Stage;
import harvester.client.profileconfig.customized.DefaultView;
import harvester.client.service.ConnectionSettingsService;
import harvester.client.service.ContributorService;
import harvester.client.service.ProcessingStepsService;
import harvester.data.Contributor;
import harvester.data.Profile;
import harvester.data.ProfileStep;
import harvester.data.Step;
import harvester.data.StepFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@SuppressWarnings("unchecked")
public class ProcessingStepsController {

    protected final Log logger = LogFactory.getLog(getClass());

    private ProcessingStepsService processingStepsService;
    
    @Autowired
	public void setProcessingStepsService(ProcessingStepsService service) {
    	processingStepsService = service;
    }
    
    private ProfileSession profilesession;

    @Autowired
    public void setProfilesession(ProfileSession ps) {
    	this.profilesession = ps;
    }

    @RequestMapping("/CheckTestProfile.htm")
    public ModelAndView checkProcessingProfile(@RequestParam("contributorid") int contributorid, HttpServletResponse response) throws IOException {
    	
    	logger.info("check profile called");

		Contributor c = processingStepsService.getContributorWithDetailedProfileInformation(contributorid);
    	
    	boolean copyable = processingStepsService.isProfileCopyable(ProfileConfigUtil.getProfileView(c.getTest()));

		response.setCharacterEncoding("UTF-8");
		ServletOutputStream outputStream = response.getOutputStream();

		outputStream.print(copyable ? "true" : "false");
		response.setStatus(HttpServletResponse.SC_OK);
		outputStream.flush();
		outputStream.close();

		return null;
    }
    
    @RequestMapping("/ViewProcessingSteps.htm")
    public ModelAndView viewProcessingSteps(@RequestParam("contributorid") int contributorid, Map model) {
    	
		Contributor c = processingStepsService.getContributorWithDetailedProfileInformation(contributorid);
		model.put("contributor", c);
		
		//so we can display the harvest and load stages on the page
		if(c.getCollection().getLoadstage() != null)
			model.put("loadstep", c.getCollection().getLoadstage().getStep());
		if(c.getHarveststage() != null)
			model.put("harveststep", c.getHarveststage().getStep());
		
		model.put("defaultprofiles", ProfileConfigUtil.getDefaultDataprofiles(c));
		
		//get the profile related stuff
		model.put("productionprofile", ProfileConfigUtil.getProfileView(c.getProduction()));
		List<Stage> testprofile = ProfileConfigUtil.getProfileView(c.getTest());
		model.put("testprofile", ProfileConfigUtil.getProfileView(c.getTest()));		
		
		model.put("testCopyable", processingStepsService.isProfileCopyable(testprofile));
		
		//the restriction types for profilesteps
		model.put("None", ProfileStep.NORMAL);
		model.put("Mandatory", ProfileStep.MANDATORY);
		model.put("Locked", ProfileStep.LOCKED);
		model.put("Enabled", ProfileStep.ENABLED);
		
    	return new ModelAndView("ViewProcessingSteps",model);
    }
    
    @RequestMapping("/StartEditingProcessingSteps.htm")
    public ModelAndView startEditingProcessingSteps(WebRequest request) throws Exception {
    	
		profilesession.initProfileSession(request);
		// generally, the profilesession object will get the profile from the database for us.
		// However, if there is already a profile in the session it will use that, which could contain
		// outdated data. So we have to "refresh" the copy basically
		Profile dp = processingStepsService.getProfileFromDb(profilesession.getProfileid());

		logger.info("profile=" + dp.getProfileid());
		profilesession.addContributorProfile(dp, profilesession.getOwnerid());
		
		return new ModelAndView(new RedirectView(profilesession.getEditProfileUrl()));    	
    }
	
    @RequestMapping("/EditProcessingSteps.htm")
    public ModelAndView editProcessingSteps(WebRequest request) throws Exception{
    	
		profilesession.initProfileSession(request);
		profilesession.getModel().put("Enabled", ProfileStep.ENABLED);
		
        return new ModelAndView("EditProcessingSteps", profilesession.getModel());
    }
    
    @RequestMapping("/EditProcessingStep.htm")
    public ModelAndView editProcessingStep(WebRequest request, 
    									   @RequestParam(value="stepid", required=false) Integer stepid,
    									   @RequestParam(value="new", required=false) String newstep
    									   ) throws Exception {
    	
		profilesession.initProfileSession(request);
		Map<String, Object> model = profilesession.getModel();
		ProfileStep stage = null;

		if( "true".equals(newstep) ) {
			logger.info("this is a new step");
			model.put("new", true);
			stage = processingStepsService.addNewProfileStepInSession(profilesession, stepid);
			
		} else {
			stage = processingStepsService.findProfileInSessionAndRefreshFolders(profilesession);
		}
					
		model.put("parameters", ProfileConfigUtil.getParameterView(stage));
		model.put("restriction", stage.getRestriction());
		logger.info("restriction = " + stage.getRestriction());
		model.put("step",stage.getStep());
		model.put("description", stage.getDescription());

		String viewname = processingStepsService.getViewOrDefault("customizedsteps/EditProcessingStep", stage.getStep().getClassname());
		
		processingStepsService.applyCustomizedPreprocessing(stage.getStep().getClassname(), model);
		
        return new ModelAndView(viewname , model);
    }
    
    @RequestMapping("/PersistMainChanges.htm")
    public ModelAndView persistMainChanges(WebRequest request, HttpServletResponse response) throws Exception {
		profilesession.initProfileSession(request);
    	
		processingStepsService.persistMainChanges(profilesession, request);
		
		response.setContentType("text/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().print("true");
		response.getOutputStream().close();
		return null;
    }
    
    @RequestMapping("/AddNewStep.htm")
    public ModelAndView addNewStep(WebRequest request) throws Exception{

		profilesession.initProfileSession(request);
		Map<String, Object> model = profilesession.getModel();

		model.put("steps", processingStepsService.getAllStepsList());		
		model.put("typemap", processingStepsService.buildTypeMap());
		
        return new ModelAndView("AddNewStep", model);
    }
    
    @RequestMapping("/AddFromCollectionPage1.htm")
    public ModelAndView addFromCollectionPageOne(WebRequest request) throws Exception {
		profilesession.initProfileSession(request);
		Map<String, Object> model = profilesession.getModel();
		
		//this should only be called from a contributor level
		
		model.put("profiles", processingStepsService.buildViewOfProfileAsParameter(profilesession));
		profilesession.setTmpProfile(
				processingStepsService.shallowCloneProfile(profilesession.getProfile()));
		
        return new ModelAndView("AddFromCollectionPage1", model);
    }
    @RequestMapping("/AddFromCollectionPage2.htm")
    public ModelAndView addFromCollectionPageTwo(WebRequest request,
    		@RequestParam("collectionprofile") int collectionprofileid,
    		@RequestParam(value="change", required=false) Boolean change) throws Exception {
    	
		profilesession.initProfileSession(request);
		Map<String, Object> model = profilesession.getModel();
		
		//add profile in		
		Profile collectionProfile = processingStepsService.getProfileFromDb(collectionprofileid);
		model.put("profile", ProfileConfigUtil.getProfileView(profilesession.getTmpProfile()));
		model.put("collectionProfileName", collectionProfile.getName());
		model.put("collectionProfile", ProfileConfigUtil.getProfileView(collectionProfile));
		model.put("collectionProfileId", collectionprofileid);
		model.put("modified", change);

        return new ModelAndView("AddFromCollectionPage2", model);
    }    
    
    @RequestMapping("/CopyProfileFromCollection.htm")
    public String copyProfileFromCollection(WebRequest request,
    		@RequestParam("collectionprofile") int collectionprofileid) throws Exception {
    	
		profilesession.initProfileSession(request);
    	
    	logger.info("copy profile from collection called");
    	
    	Profile collectionProfile = processingStepsService.getProfileFromDb(collectionprofileid);
    	profilesession.getTmpProfile().getProfilesteps().clear();
    	for(ProfileStep ps : collectionProfile.getProfilesteps()) {
    		processingStepsService.copyProfileStepToEnd(ps, profilesession.getTmpProfile());
    	}
    	
    	return "redirect:" + profilesession.getAddFromCollectionPageTwoUrl() 
    		+ "&collectionprofile=" + collectionprofileid + "&change=true";
    }
    
    @RequestMapping("/CopyStepFromCollection.htm")
    public String copyStepFromCollection(WebRequest request,
    		@RequestParam("collectionprofile") int collectionprofileid,
    		@RequestParam("psid") int psid) throws Exception {
    	
		profilesession.initProfileSession(request);
    	
    	logger.info("copy step from collection called, psid=" + psid);
    	
    	Profile collectionProfile = processingStepsService.getProfileFromDb(collectionprofileid);
    	ProfileStep copyStep = null;
    	for(ProfileStep ps : collectionProfile.getProfilesteps())
    		if(ps.getPsid().equals(psid))
    			copyStep = ps;
    	
    	processingStepsService.copyProfileStepToEnd(copyStep, profilesession.getTmpProfile());
    	
    	return "redirect:" + profilesession.getAddFromCollectionPageTwoUrl() 
    		+ "&collectionprofile=" + collectionprofileid  + "&change=true";
    }
    
    @RequestMapping("/PersistFromCollectionProfileChanges.htm")
    public String persistFromCollectionProfileChanges(WebRequest request) throws Exception {
		profilesession.initProfileSession(request);
		
		profilesession.copyTmpToMain();
		
		return "redirect:" + profilesession.getEditProfileUrl();
    }
    
    @RequestMapping("/DeleteCollectionProfile.htm")
    public String deleteCollectionProfile(WebRequest request ) throws Exception {
    	
		profilesession.initProfileSession(request);
    	processingStepsService.deleteCollectionProfile(profilesession.getProfileid(), profilesession.getOwnerid());
    	
    	return "redirect:" + profilesession.getReturnurl();
    }
    
    @RequestMapping("/ChangeProfileStepPosition.htm")
    public String changeProfileStepPosition(WebRequest request) throws Exception {
    	profilesession.initProfileSession(request);
		String direction = request.getParameter("direction");		
		processingStepsService.changeStagePosition(profilesession.getPosition(), direction, profilesession.getProfile());
		
		return "redirect:" + profilesession.getEditProfileUrl();		
    }
    
    @RequestMapping("/DeleteProfileStep.htm")
    public String deleteProfileStep(WebRequest request) throws Exception {
    	profilesession.initProfileSession(request);
		ProfileConfigUtil.removeFromPipeline(profilesession.getProfile(), profilesession.getPosition());				
		return "redirect:" + profilesession.getEditProfileUrl();	
    }
    
    @RequestMapping("/GoBackInStepWizard.htm")
    public String goBackInStepWizard(WebRequest request) throws Exception {
    	profilesession.initProfileSession(request);
		ProfileConfigUtil.removeFromPipeline(profilesession.getProfile(), profilesession.getPosition());				
		return "redirect:" + profilesession.getAddNewUrl();	
    }
    @RequestMapping("/SaveProfile.htm")
    public String saveProfile(WebRequest request, @RequestParam(value="profilename", required=false) String profilename, 
    											  @RequestParam(value="profiletype", required=false) Integer profiletype) throws Exception {
    	profilesession.initProfileSession(request);
    	processingStepsService.persistMainChanges(profilesession, request);
    	processingStepsService.saveProfile(profilesession, profilename, profiletype);    	
    	return "redirect:" + profilesession.getReturnurl();
    }
    
    @RequestMapping("/PreserveProfileEdit.htm")
    public String preserveProfileEdit(HttpServletRequest request) throws Exception {
    	return "redirect:" + processingStepsService.preserveEditToSession(request, profilesession);
    }
    
    @RequestMapping("/PlainTextProfileView.htm")
    public ModelAndView plainTextProfileView(WebRequest request) throws Exception {
    	profilesession.initProfileSession(request);
    	Map<String, Object> model = profilesession.getModel();
    	
    	List<String> renderedSteps = processingStepsService.renderSteps(profilesession.getProfile().getProfilesteps());
    	
    	model.put("renderedSteps", renderedSteps);
    	
    	return new ModelAndView("customizedsteps/PlainTextProfileView", model);
    }
    
}
