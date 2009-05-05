package harvester.client.profileconfig.customized;

import harvester.client.connconfig.*;
import harvester.client.profileconfig.ICustomizedStep;

import java.util.*;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



// All Client side code for the AddField2 step is in here.
@SuppressWarnings("unchecked")
public class AddField2 implements ICustomizedStep {

    protected final Log logger = LogFactory.getLog(getClass());

    //This does basically nothing, just the bare minimum
	public Map PostProcess(Map inmap) {
		logger.info("AddField2 postprocessor called");
		Map newmap = new HashMap();
		newmap.putAll(inmap);
		return newmap;
	}

	
	/**
	 * if they enter something in the value matches field, we save that, but don't use it
	 * next time they open the page we don't show it. This is the easiest way to handle this
	 * sort of thing.
	 */
	public void PreProcess(Map<String, Object> model) {
		logger.info("AddField2 preprocesser called");
		
		List<StepParameterView> steps = (List<StepParameterView>) model.get("parameters");
		
		boolean valueMatchNeeded = false;
		
		for(StepParameterView spv : steps)
			if(spv.getName().equals("Add"))
				if(spv.getValue() != null && spv.getValue().equals("4"))
					valueMatchNeeded = true;
		
		if(!valueMatchNeeded)
		{
			for(StepParameterView spv : steps)
				if(spv.getName().equals("Value Matches"))
					spv.setValue("");
		}
	}

}
