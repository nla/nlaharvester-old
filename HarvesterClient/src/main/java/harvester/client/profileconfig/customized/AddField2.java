package harvester.client.profileconfig.customized;

import harvester.client.connconfig.*;
import harvester.client.profileconfig.ICustomizedStep;
import harvester.client.profileconfig.ParameterComparator;
import harvester.data.ProfileStep;
import harvester.data.ProfileStepParameter;

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


	public String renderPlainTextView(ProfileStep ps) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<dl>\n");
		
		ProfileStepParameter[] parameters = ps.getParameters().toArray(new ProfileStepParameter[0]);
		ParameterComparator comp = new ParameterComparator();
		
		
		Arrays.sort(parameters, comp);
		
		for(int i = 0 ; i < parameters.length; i++) {
			String psp_name = parameters[i].getPis().getParametername();
			String psp_value = parameters[i].getValue();
			
			if(psp_name.equals("Add")) {
				sb.append("<dt>Add Option</dt>\n");

				if(psp_value != null && !psp_value.equals("")) {
					int option = Integer.valueOf(psp_value);
					
					switch(option) {
						case 0: sb.append("<dd>Always</dd>\n"); break;
						case 1: sb.append("<dd>If match field is present</dd>\n"); break;
						case 2: sb.append("<dd>If match field is not present</dd>\n"); break;
						case 3: sb.append("<dd>If match field is not present or is empty</dd>\n"); break;
						case 4: sb.append("<dd>If match field value matches</dd>\n"); break;
					}
				} else {
					sb.append("<dd>Add not configured</dd>\n");
				}
			} else if(psp_name.equals("Value Matches") && psp_value == null){
				//don't display
			} else {
				sb.append("<dt>" + psp_name + "</dt>\n");
				sb.append("<dd>" + psp_value + "</dd>\n");
			}
		}
		
		sb.append("</dl>\n");
		
		return sb.toString();
	}

}
