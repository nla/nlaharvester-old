package harvester.client.web;

import java.util.List;
import java.util.Map;

import harvester.client.connconfig.ConnectionSettings;
import harvester.client.connconfig.PUtil;
import harvester.client.connconfig.StepParameterView;
import harvester.client.service.ConnectionSettingsService;
import harvester.client.service.ContributorService;
import harvester.client.util.ControllerUtil;
import harvester.client.util.EscapeHTML;
import harvester.client.util.KeyValue;
import harvester.data.Contributor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

@Controller
@SuppressWarnings("unchecked")
public class ConnectionSettingsController {

    protected final Log logger = LogFactory.getLog(getClass());
    private ContributorService contributorService;
    private ConnectionSettingsService connectionSettingsService;
    
    @Autowired
	public void setContributorService(ContributorService contributorService) {
		this.contributorService = contributorService;
	}
	
    @Autowired
    public void setConnectionSettingsService(ConnectionSettingsService connectionSettingsService) {
    	this.connectionSettingsService = connectionSettingsService;
    }
    
    @RequestMapping("/ViewConnectionSettings.htm")
    public String viewConnectionSettings(@RequestParam("contributorid") int contributorid, Map model) {
    	
		Contributor c = connectionSettingsService.getContributorForSettings(contributorid);
		model.put("contributor", c);
		
		//if there is no harvest stage we leave it null, then the view will know to show a message
		if(c.getHarveststage() != null) {
			model.put("connectiontype", c.getHarveststage().getStep().getName());
			
			//now we just need to convert the profile format of key values into a simple list
			List<KeyValue> kvs = PUtil.getKeyValueList(c.getHarveststage());
			
			model.put("settings", kvs);
		}
    	
    	return "ViewConnectionSettings";
    }
    
    @RequestMapping("/StartConnectionSettingsWizard.htm")
    public String startConnectionSettingsWizard(@RequestParam("contributorid") int contributorid) {
    	
    	ConnectionSettings cs = contributorService.addExistingContributorToSession(contributorid);
    	connectionSettingsService.InitializeConnectionSettingsWizard(cs);
    	
    	return "redirect:EditConnectionSettingsStep1.htm?contributorid=" + contributorid;
    }
    
    @RequestMapping("/EditConnectionSettingsStep1.htm")
    public String editConnectionSettingsStep1(@RequestParam("contributorid") int contributorid, Map model) {
    	
    	//just retrieve the settings from the session and create the model for that.
    	ConnectionSettings cs = connectionSettingsService.getConnectionSettingsFromSession(contributorid);
    	
		model.put("selectedtype", cs.getStepid() );		
		model.put("contributor", cs.getC());		
		model.put("harvesttypes", contributorService.getHarvestTypes(cs.getC().getCollection()));
		model.put("new", cs.isNewContributor());
		
    	return "EditConnectionSettingsStep1"; 
    }
    
    @RequestMapping("/PersistConnectionSettingsStep1.htm")
    public String persistConnectionSettingsStep1(@RequestParam("contributorid") int contributorid,
    											 @RequestParam("harvesttype") int harvesttype) {
    	
    	contributorService.saveHarvestTypeInSession(contributorid, harvesttype);
    	
    	return "redirect:EditConnectionSettingsStep2.htm?contributorid=" + contributorid;
    }
    
    @RequestMapping("/EditConnectionSettingsStep2.htm")
    public String editConnectionSettingsStep2(@RequestParam("contributorid") int contributorid, WebRequest request, Map model) {
    	
    	ConnectionSettings cs = connectionSettingsService.getConnectionSettingsFromSession(contributorid);
    	
		//get the contributor related stuff from the session
		model.put("contributor", cs.getC());		
		model.put("harvesttype", cs.getStepName());
		model.put("new", cs.isNewContributor());
		
		List<StepParameterView> parameters = connectionSettingsService.getInitialParameters(cs);

		if(parameters == null) {
			//return persistConnectionSettingsStep2(contributorid, request);
			return "redirect:EditConnectionSettingsStep3.htm?new=true&contributorid=" + contributorid;
		} else {
			
			//if any parameters have been selected already we fill that in from the session's settings
			for(StepParameterView sp : parameters) {
				String value = cs.getInitialproperties().get(sp.getId());
				if( value != null)
					sp.setValue(value);
			}
			
			model.put("parameters", parameters);
			
	    	return "EditConnectionSettingsStep2"; 
		}
    }
    
    @RequestMapping("/PersistConnectionSettingsStep2.htm")
    public String persistConnectionSettingsStep2(@RequestParam("contributorid") int contributorid, WebRequest request) {
    	
    	ConnectionSettings cs = connectionSettingsService.getConnectionSettingsFromSession(contributorid);
    	
    	Map<String, String> requestMap = ControllerUtil.ConvertRequestToMap(request);
    	//update our session with the entered data from the user
    	cs.getInitialproperties().putAll( connectionSettingsService.filterAndBuildInitialsMap(requestMap) );	
    	
    	return "redirect:EditConnectionSettingsStep3.htm?contributorid=" + contributorid;
    }
    
    @RequestMapping("/EditConnectionSettingsStep3.htm")
    public String editConnectionSettingsStep3(@RequestParam("contributorid") int contributorid, Map model) {
    	
    	//just retrieve the settings from the session and create the model for that.
    	ConnectionSettings cs = connectionSettingsService.getConnectionSettingsFromSession(contributorid);

		model.put("contributor", cs.getC());		
		model.put("harvesttype", cs.getStepName());
		model.put("new", cs.isNewContributor());
		
		List<StepParameterView> parameters = connectionSettingsService.getAllParameters(cs);
    	
		logger.info("other parameters num: " + cs.getOtherproperties().size());

		for(StepParameterView sp : parameters) {
			logger.info("Step3: param: sp.id=" + sp.getId() + " name=" + sp.getName());
			String value = cs.getOtherproperties().get(sp.getId());
			logger.info("value: " + value);
			if( value != null)
				sp.setValue(value);
		}
		
		connectionSettingsService.AddInitialParametersIntoOtherParameterMap(parameters, cs.getInitialproperties());

		model.put("fetched", connectionSettingsService.applyStepDepedentModifcationsOfParameters(parameters, contributorid));
		
		//the third step shows the drop down box where they can select the profile for this contributor ONLY when this is 
		//a new contributor's settings wizard. So we need to handle it here
		if(cs.isNewContributor()) 
			parameters.add(connectionSettingsService.buildViewOfProfileAsParameter(contributorid));
		
		//do escaping
		for(StepParameterView sp : parameters) {
			sp.setValue(EscapeHTML.forHTML(sp.getValue()));
		}
		
		model.put("parameters", parameters);
		
    	return "EditConnectionSettingsStep3"; 
    }
    
    @RequestMapping("/PersistConnectionSettingsStep3.htm")
    public String persistConnectionSettingsStep3(@RequestParam("contributorid") int contributorid, WebRequest request) {
    	
    	ConnectionSettings cs = connectionSettingsService.getConnectionSettingsFromSession(contributorid);
    	
    	Map<String, String> requestMap = ControllerUtil.ConvertRequestToMap(request);
    	//update our session with the entered data from the user
    	Map<Integer, String> parameters = connectionSettingsService.filterAndBuildInitialsMap(requestMap);
    	
    	//do some step dependent post processing
    	connectionSettingsService.applyParameterPostProcessing(parameters, contributorid);
    	
    	//if we displayed the profile dropdown then they will have selected a profile, so we set that in the session
    	if(parameters.containsKey(StepParameterView.PROFILE_ID)) {
    		connectionSettingsService.setSelectedProfile(contributorid, Integer.valueOf(parameters.get(StepParameterView.PROFILE_ID)));
    		parameters.remove(StepParameterView.PROFILE_ID);
    	}
    	
    	connectionSettingsService.setFinalParameterValuesInSession(contributorid, parameters);
    	
    	////if this is not a new contributor we don't have to worry about doing a full save
    	//if(cs.isNewContributor())
    	contributorService.SaveContributor( contributorid, cs.isNewContributor() );
    	
    	//here the contributor may have a new contributorid, but we need to old id to extract some more info
    	//out of the session.
    	contributorService.SaveConnectionSettings(contributorid);    	
    	
    	return "redirect:ViewContributor.htm?contributorid=" + cs.getC().getContributorid();
    }
    
}
