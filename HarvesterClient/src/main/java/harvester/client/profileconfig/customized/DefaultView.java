package harvester.client.profileconfig.customized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import harvester.client.profileconfig.ICustomizedStep;
import harvester.client.profileconfig.ParameterComparator;
import harvester.data.ProfileStep;
import harvester.data.ProfileStepParameter;

public class DefaultView implements ICustomizedStep {

	protected final Log logger = LogFactory.getLog(getClass());
	
	public Map PostProcess(Map inmap) {
		return inmap;
	}

	public void PreProcess(Map<String, Object> model) {
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
			
			if(parameters[i].getPis().getNested().size() == 0) {
				logger.info("displaying piid=" + parameters[i].getPis().getPiid() + " gli=" + parameters[i].getGrouplistindex());
				
				Integer before_gli = i == 0 ? null : parameters[i-1].getGrouplistindex();
				Integer this_gli = parameters[i].getGrouplistindex();
				if(before_gli != null && (!before_gli.equals(this_gli))) {
					logger.info("blankline");
					sb.append("<dt class=\"dt-sep\">&nbsp;</dt><dd>&nbsp;</dd>\n");
				}
				
				sb.append("<dt>" + psp_name + "</dt>\n");
				sb.append("<dd>" + psp_value + "</dd>\n");
			} else {
				logger.info("not displaying piid=" + parameters[i].getPis().getPiid());
			}
		}
		
		sb.append("</dl>\n");
		
		return sb.toString();
	}

}
