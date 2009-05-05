package harvester.client.service;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import harvester.client.connconfig.ConnectionSettings;
import harvester.client.connconfig.InputPluginConfigurer;
import harvester.client.connconfig.PUtil;
import harvester.client.connconfig.SessionConnectionHandler;
import harvester.client.connconfig.StepParameterView;
import harvester.client.data.dao.DAOFactory;
import harvester.client.util.KeyValue;
import harvester.data.Contributor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectionSettingsService {
	
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
	 * Get the specified contributor from the database along with all the information about 
	 * its connection settings
	 * @param contributorid
	 */
	public Contributor getContributorForSettings(int contributorid) {
		return daofactory.getContributorDAO().getContributorAndHarvestStepDetails(contributorid);
	}
	
	/**
	 * retrieve the connection settings corresponding to the specified contributor from the session.
	 * @param contributorid
	 */
	public ConnectionSettings getConnectionSettingsFromSession(int contributorid) {
		return sessionconnectionhandler.getConnectionSetting(contributorid);
	}
	
	/**
	 * Get the initial parameters for the harvest type from the database.
	 * These correspond to parameters that should be shown on step 2 of the wizard,
	 * since they specify information needed to build page 3 of the wizard.
	 * @param cs 
	 * @return list of parameters in a view friendly form
	 */
	public List<StepParameterView> getInitialParameters(ConnectionSettings cs) {
		
		Integer stepid = cs.getStepid();
		String stepname = daofactory.getStepDAO().getStep(stepid).getClassname();

		return inputpluginconfigurer.getInitialParameters(stepname, stepid, cs.getC());
	}
	/**
	 * Get all parameters for the given harvest type from the database.
	 * @param cs
	 * @return
	 */
	public List<StepParameterView> getAllParameters(ConnectionSettings cs) {
		
		Integer stepid = cs.getStepid();
		String stepname = daofactory.getStepDAO().getStep(stepid).getClassname();

		return inputpluginconfigurer.getAllParameters(stepid, cs.getC());
	}
	
	/**
	 * Do various initialization for the connection settings wizard.
	 * @param cs
	 */
	public void InitializeConnectionSettingsWizard(ConnectionSettings cs) {
		
		//when editing existing connection settings we need to fill in some stuff in the session
		
		List<KeyValue> kvs = PUtil.getKeyValueList(cs.getC().getHarveststage(), PUtil.INTEGER_KEYS);
		
		cs.setStep(cs.getC().getHarveststage().getStep());
		
		//TODO: I'm not sure why this is here. CLARIFY why its needed.
		for(KeyValue kv : kvs)
		{
			//any key that should be in the initial map will be in the map we get from inputpluginconfigurer
			//this is O(n*m) in the two list sizes, but they are both very small.
			Integer ikey = Integer.valueOf(kv.getKey());
			if(inputpluginconfigurer.getInitialFieldsForInputStage(cs.getStepClassName()).contains(ikey))
				cs.getInitialproperties().put(ikey, kv.getValue());
			cs.getOtherproperties().put(Integer.valueOf(kv.getKey()), kv.getValue());
		}
		
	}

	/**
	 * Filters out contributorid and new from the map as well as converting the keys into integers.
	 * @param pmap
	 * @return
	 */
	public Map<Integer, String> filterAndBuildInitialsMap(Map<String, String> pmap) {
		//each form field returned is the id of a parameterinfo tuple, except for contributorid of course
		Map<Integer, String> initials = new HashMap<Integer, String>();
		
		for(Entry<String, String> entry : pmap.entrySet()) {
			
			if(entry.getKey().equals("contributorid") || entry.getKey().equals("new"))
				continue;	//skip it
			
			initials.put(Integer.valueOf(entry.getKey()), entry.getValue());
		}
	
		return initials;
	}
	
	/**
	 * Fills in the values of a step parameter view with the data from the parameter map
	 * @param spv step parameter view to fill in the values of
	 * @param pamap parameter map with which to lookup values in.
	 */
	public void AddInitialParametersIntoOtherParameterMap(List<StepParameterView> spv, HashMap<Integer, String> pamap) {
		
		//we just loop over each parameterview, checking if it is in the passed map as we go
		for(StepParameterView sp : spv) {
			String value = pamap.get(sp.getId());
			if(value != null) {
				sp.setValue(value);
				sp.setType(sp.READ_ONLY);
			}
		}
		
	}
	
	/**
	 * Using the plugin for the selected stepid from step 1 we get it to modify the parameters however it needs.
	 * If the step encountered some sort of error we return false
	 * @param spv list of parameters already selected in previous steps
	 * @param contributorid
	 * @return has error occured?
	 */
	public boolean applyStepDepedentModifcationsOfParameters(List<StepParameterView> spv, int contributorid) {
		ConnectionSettings cs = getConnectionSettingsFromSession(contributorid);
		return inputpluginconfigurer.fetchStepDependentInfo(cs.getStepClassName(), cs.getStepid(), spv, cs.getC());
	}
	
	public void applyParameterPostProcessing(Map<Integer, String> parameters, int contributorid) {
		ConnectionSettings cs = getConnectionSettingsFromSession(contributorid);
		inputpluginconfigurer.doPostProcessing(cs.getStepClassName(), cs.getStepid(), parameters, cs.getC());
	}
	
	
	/**
	 * puts the selected profile in the session.
	 * @param contributorid
	 * @param profileid
	 */
	public void setSelectedProfile(int contributorid, int profileid) {
		sessionconnectionhandler.getConnectionSetting(contributorid).setSelectedProfile(profileid);
	}
	
	/**
	 * Stores the given parameter map in the session for use later.
	 * @param contributorid
	 * @param parameters
	 */
	public void setFinalParameterValuesInSession(int contributorid, Map<Integer, String> parameters) {
		sessionconnectionhandler.getConnectionSetting(contributorid).setFinalvalues(parameters);
	}
	
	/**
	 * Step 3 of the wizard need to have a dropdown box of profiles that can be used for the given contributor.
	 * We build this here in the form of a step parameter view. Normally these correspond directly to database
	 * fields, so this is somewhat of an exception to the norm.
	 * @param contributorid
	 * @return view of the profile.
	 */
	public StepParameterView buildViewOfProfileAsParameter(int contributorid) {
		ConnectionSettings cs = getConnectionSettingsFromSession(contributorid);
		
		StepParameterView profilespv = new StepParameterView();
		profilespv.setName("Processing Profile:");
		profilespv.setId(StepParameterView.PROFILE_ID);	//profilenumber
		profilespv.setEditibility(1);	//means required
		LinkedList<KeyValue> profiles = daofactory.getCollectionDAO()
			.getDefaultProfilesForCollection(cs.getC().getCollection().getCollectionid(), cs.getC().getHtype());
		
		if(profiles.size() == 0) {
			profilespv.setType(StepParameterView.READ_ONLY_HIDDEN);
			profilespv.setHiddenvalue("-1");	//means create a blank
			profilespv.setValue("NONE");
			
		} else if (profiles.size() == 1) {
			profilespv.setType(StepParameterView.READ_ONLY_HIDDEN);
			profilespv.setHiddenvalue(profiles.getFirst().getKey());
			profilespv.setValue(profiles.getFirst().getValue());
		} else {
			profilespv.setType(StepParameterView.DROP_DOWN);
		}
			
		profiles.addFirst(new KeyValue("", "&nbsp;"));	//this is the empty entry that will appear first in the dropdown
		profilespv.setOptions(profiles);	//not used unless this is a dropdown one
		
		return profilespv;
	}
	
}
