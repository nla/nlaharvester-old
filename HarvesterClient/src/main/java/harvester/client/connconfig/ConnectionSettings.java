package harvester.client.connconfig;

import harvester.client.util.KeyValue;
import harvester.data.Contributor;
import harvester.data.Step;

import java.util.*;


/**This class holds the current state of the data entered during the connection settings wizard for
 * a single contributor while the user navigates through the wizard. If they have multiple wizards
 * open for different contributors at a time several of these objects will be in existance, being 
 * held in the sessionconnectionhandler class.
 */

public class ConnectionSettings {
	
	/** is this a new contributor's wizard we are in? */
	private boolean newContributor;

	/** a cache of the selected step from the first bit of the wizard */
	private Step step;
	
	/**properties entered in the second stage of the wizard */
	private HashMap<Integer, String> initialproperties = new HashMap<Integer, String>();
	/**properties entered in both stages, filled in by the third stage */
	private HashMap<Integer, String> otherproperties = new HashMap<Integer, String>();

	/** 
	When the connection settings entered are finally saved, this is passed into A DAO as follows
	daofactory.getContributorDAO().AddNewHarvestStage(cid, stepid, kvs);	where kvs is finalvalues
	Its values are filled out directly from the 3rd stages form submitted response. in 
	EditConnectionSettings3Controller.
	*/
	private Map<Integer, String> finalvalues = new HashMap<Integer, String>();
	
	private Integer selectedProfile;

	private Contributor c; 
	
	
	
	public boolean isNewContributor() {
		return newContributor;
	}

	public void setNewContributor(boolean newContributor) {
		this.newContributor = newContributor;
	}
	
	public Map<Integer, String> getFinalvalues() {
		return finalvalues;
	}

	public void setFinalvalues(Map<Integer, String> finalvalues) {
		this.finalvalues = finalvalues;
	}
	public Contributor getC() {
		return c;
	}

	public void setC(Contributor c) {
		this.c = c;
	}
	
	public Step getStep() {
		return step;
	}

	public void setStep(Step step) {
		this.step = step;
	}
	
	public Integer getStepid() {
		return step.getStepid();
	}

	
	public ConnectionSettings() {
		//do nothing
	}

	public String getStepName() {
		return step.getName();
	}

	public HashMap<Integer, String> getInitialproperties() {
		return initialproperties;
	}

	public void setInitialproperties(HashMap<Integer, String> initialproperties) {
		this.initialproperties = initialproperties;
	}

	public HashMap<Integer, String> getOtherproperties() {
		return otherproperties;
	}

	public void setOtherproperties(HashMap<Integer, String> otherproperties) {
		this.otherproperties = otherproperties;
	}

	public Integer getSelectedProfile() {
		return selectedProfile;
	}

	public void setSelectedProfile(Integer selectedProfile) {
		this.selectedProfile = selectedProfile;
	}

	public String getStepClassName() {
		return step.getClassname();
	}
	
	
}
