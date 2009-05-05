package harvester.client.data.dao.interfaces;

import harvester.data.Step;

import java.util.List;


public interface StepDAO {
	public List<Step> getInputSteps();
	public Step getStep(int stepid);
	public Step getStep(int stepid, boolean options, boolean parameterInfos);
	public List<Step> getAllSteps();
	public String getParameterValue(int parameterid);
	
}
