package harvester.client.profileconfig.customized;

import harvester.client.connconfig.*;
import harvester.client.profileconfig.ICustomizedStep;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;






////////////////////////////////DEPRECIATED/////////////////////////////////////////




@SuppressWarnings("unchecked")
public class AddField implements ICustomizedStep {

    protected final Log logger = LogFactory.getLog(getClass());
	
    private String fieldvaluepiid;
    
    
	public String getFieldvaluepiid() {
		return fieldvaluepiid;
	}

	public void setFieldvaluepiid(String fieldvaluepiid) {
		this.fieldvaluepiid = fieldvaluepiid;
	}

	public Map PostProcess(Map inmap) {
		logger.info("AddField postprocessor called");
		String fieldvalue = (String)inmap.get(fieldvaluepiid);
		String enteredvalue = (String)inmap.get("enteredvalue");
		
		Map newmap = new HashMap();
		newmap.putAll(inmap);
		
		if("entered".equals(fieldvalue))
		{
			newmap.put(fieldvaluepiid, enteredvalue == null ? "" : enteredvalue);
			
			newmap.remove("enteredvalue");
		}
		
		return newmap;

	}

	public void PreProcess(Map<String, Object> model) {
		logger.info("AddField preprocesser called");
		
		List<StepParameterView> steps = (List<StepParameterView>) model.get("parameters");
		
		for(StepParameterView spv : steps)
		{
			if(spv.getName().equals("Field Value"))
			{
				if(spv.getValue() != null && !spv.getValue().equals("orgname"))
				{
					logger.info("user entered values are being used");
					model.put("enteredvalue", spv.getValue());
					spv.setValue("");
				}
			}
		}
		
	}

}
