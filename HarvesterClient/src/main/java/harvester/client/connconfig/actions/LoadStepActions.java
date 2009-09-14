package harvester.client.connconfig.actions;

import harvester.client.util.KeyValue;
import harvester.data.ProfileStep;

import java.util.List;

/**
 * This class provides an interface that can(and normally should) be implemented for all datastores. 
 * It provides a few functions needed by the user interface.
 */
public interface LoadStepActions {

	public void deleteProductionRecords(int contributorid, String contributorname);
	public Integer getCollectionSize();
	
	/**
	 * Anything returned here will be displayed to the user on the collection details page.
	 * If null is returned, no settings will be displayed
	*/
	public List<KeyValue> getSettings(ProfileStep load_step);
	
}
