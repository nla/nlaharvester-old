package harvester.client.profileconfig;

import harvester.client.data.dao.DAOFactory;

import java.util.Map;

/**
 * A bean filled in by the IOC container, which holds links between the id of steps and Client 
 * side specific code for those steps, and there view files. The code is in the form of classes in profileconfig.customized
 * that implement the interface ICustomizedStep.
 */
public class StepPluginConfigurer {

	/** map of customized view names mapped from stepid -> view name */
	private Map<String, String> alternateviews;
	/** map of custom step objects, step -> object */
	private Map<String, ICustomizedStep> customizedSteps;
    private DAOFactory daofactory;
  

	public Map<String, ICustomizedStep> getCustomizedSteps() {
		return customizedSteps;
	}

	public void setCustomizedSteps(Map<String, ICustomizedStep> customizedSteps) {
		this.customizedSteps = customizedSteps;
	}

	public DAOFactory getDaofactory() {
		return daofactory;
	}

	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

	public Map<String, String> getAlternateviews() {
		return alternateviews;
	}

	public void setAlternateviews(Map<String, String> alternateviews) {
		this.alternateviews = alternateviews;
	}
	
}
