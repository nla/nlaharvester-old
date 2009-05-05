package harvester.client.connconfig;

import harvester.client.connconfig.actions.StepOptionProcessor;
import harvester.client.data.dao.DAOFactory;
import harvester.client.profileconfig.ProfileConfigUtil;
import harvester.client.util.KeyValue;
import harvester.data.Contributor;
import harvester.data.ParameterInformation;
import harvester.data.ParameterOption;
import harvester.data.Step;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Primarly handles step specific stuff that needs to be done in the connection settings wizard
 * For example, OAI has a implementation of IstepOptionProcessor that will be in the linked list
 * below, and also specifies some fields to appear on the 2nd stages screen, which is controlled by
 * initalparameters map.
 */
public class InputPluginConfigurer {

    private static Log logger = LogFactory.getLog(InputPluginConfigurer.class);
	
	/** A map from stepname to the Inital parameters to show on step 2 of the wizard when that step is being used. */ 
	private Map<String, List<Integer>> initialparameters;
	/** A map from stepname to a class that can processes the initial parameters entered by the user in the connection wizard. */
	private Map<String, StepOptionProcessor> processors;
	
	public Map<String, List<Integer>> getInitialparameters() {
		return initialparameters;
	}

    private DAOFactory daofactory;
    
	public DAOFactory getDaofactory() {
		return daofactory;
	}

	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	public void setInitialparameters(Map<String, List<Integer>> initialparameters) {
		this.initialparameters = initialparameters;
	}


	public List<Integer>  getInitialFieldsForInputStage(String stepid)
	{
		return initialparameters.get(stepid); 
	}
	
	
	/**
	 * Builds a view of the intial parameters needed for the passed step when used in a connection settings
	 * wizard. 
	 * @param stepid the step they selected on the first page of the wizard, specifically the harvest type we are using
	 * @param c a contributor data object.
	 * @return A step parameter view list of the initial parameters.
	 */
	public List<StepParameterView> getInitialParameters(String stepname, int stepid, Contributor c)
	{
		LinkedList<StepParameterView> params = new LinkedList<StepParameterView>();
		Set<ParameterInformation> pis = null;
		
		//the contributor object might have all the step parameter stuff already
		if(c.getHarveststage() != null && c.getHarveststage().getStep() != null)
			pis = c.getHarveststage().getStep().getPis();
		else
		{
			//otherwise we just have to get them from the database
			Step s = daofactory.getStepDAO().getStep(stepid, false, true);	//gets parameter options as well
			pis = s.getPis();
		}
		List<Integer> initials = getInitialFieldsForInputStage(stepname);
		
		for(ParameterInformation pi : pis)
		{
			//is it one of the initial parameters?
			if(initials.contains(pi.getPiid()))
			{
				logger.info("found initial parameter: " + pi.getParametername());
				//fill out one of the stepparameterview objects then!
				StepParameterView spv = new StepParameterView();
				spv.setId(pi.getPiid());
				spv.setName(pi.getParametername());
				spv.setEditibility(pi.getEditibility());
				spv.setDescription(pi.getDescription());
				
				spv.setValue(pi.getDefaultvalue());
				
				if(pi.getOptions() != null && pi.getOptions().size() > 0)
				{		
					logger.info("detecting as dropdown" + pi.getParametername());
					spv.setOptions(new LinkedList<KeyValue>());
					//For the moment we don't need radio buttons
					//if(pi.getEditibility() == 2)
					spv.setType(spv.DROP_DOWN);
					//else
					//	spv.setType(spv.RADIO);
					for(ParameterOption po : pi.getOptions())
						spv.getOptions().add(new KeyValue(po.getValue(), po.getDescription()));
				} 
				
				params.add(spv);
			}
		}
		return params;
	}


	/**
	 * Builds the parameter view for the third page of the wizard, containing readonly fields for the pararameters
	 * entered in the seconds step and writable fields for the extra information they need to provide in this step
	 * @param stepid  stepid the step they selected on the first page of the wizard, specifically the harvest type we are using
	 * @param c contributor data object
	 * @return A step parameter view list of the initial parameters.
	 */
	public List<StepParameterView> getAllParameters(int stepid, Contributor c) {
		
		LinkedList<StepParameterView> params = new LinkedList<StepParameterView>();
		
		Set<ParameterInformation> pis = null;
		
		//the contributor object might have all the step parameter stuff already
		if(c.getHarveststage() != null && c.getHarveststage().getStep() != null)
			pis = c.getHarveststage().getStep().getPis();
		else
		{
			//otherwise we just have to get them from the database
			Step s = daofactory.getStepDAO().getStep(stepid, false, true);
			pis = s.getPis();
		}
		
		for(ParameterInformation pi : pis)
		{
			//fill out one of the stepparameterview objects then!
			StepParameterView spv = new StepParameterView();
			spv.setId(pi.getPiid());
			spv.setName(pi.getParametername());
			spv.setEditibility(pi.getEditibility());
			spv.setDescription(pi.getDescription());
			//TODO: required needs adding				
			spv.setValue(pi.getDefaultvalue());
			if(pi.getEditibility() == ParameterInformation.READ_ONLY)
				spv.setType(spv.READ_ONLY);
			else if(pi.getEditibility() == ParameterInformation.HIDE)
				spv.setType(spv.READ_ONLY_HIDDEN);
			else
				spv.setType(spv.TEXT);
			params.add(spv);
		}
		return params;
	}
	
	/**
	 * Modifies the parameter view in a step specific manor, delegating the modifications to a implementation of StepOptionProcessor for that step.
	 * @param stepid  stepid the step they selected on the first page of the wizard, specifically the harvest type we are using
	 * @param c contributor data object
	 * @param spv a paramter view to manipulate
	 * @return success/failure
	 */
	public boolean fetchStepDependentInfo(String stepname, int stepid, List<StepParameterView> spv, Contributor c)
	{
		logger.info("fetching step info for step, stepid=" + stepid + " stepname=" + stepname);
		//called after the above two functions, generally
		//we just delegate the actions to the option processor for this step
		StepOptionProcessor sop = processors.get(stepname);
		if(sop != null)
			return sop.process(spv, daofactory, c);
		else 
			return false;
	}


	public Map<String, StepOptionProcessor> getProcessors() {
		return processors;
	}


	public void setProcessors(Map<String, StepOptionProcessor> processors) {
		this.processors = processors;
	}

	public void setHtype(Contributor c, String stepname) {
		
		c.setHtype(processors.get(stepname).getHtype());
	}

	public void doPostProcessing(String stepname, Integer stepid, Map<Integer, String> parameters, Contributor c) {
		logger.info("doing post processing for a step, stepid=" + stepid + " stepname=" + stepname);
		//we just delegate the actions to the option processor for this step
		StepOptionProcessor sop = processors.get(stepname);
		if(sop != null)
			sop.postProcess(parameters, daofactory, c);
	}
	
}
