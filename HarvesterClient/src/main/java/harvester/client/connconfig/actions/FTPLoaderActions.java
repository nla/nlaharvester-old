package harvester.client.connconfig.actions;

import harvester.client.util.KeyValue;
import harvester.data.ProfileStep;
import harvester.data.ProfileStepParameter;

import java.util.LinkedList;
import java.util.List;

public class FTPLoaderActions implements LoadStepActions {
	
	public void deleteProductionRecords(int contributorid, String contributorname) {
		//not supported
	}

	public Integer getCollectionSize() {
		//not supported
		return null;
	}

	public List<KeyValue> getSettings(ProfileStep load_step) {
		LinkedList<KeyValue> settings = new LinkedList<KeyValue>();
		
		for(ProfileStepParameter psp : load_step.getParameters()) {
			settings.add(new KeyValue(psp.getPis().getParametername(), psp.getValue()));
		}
		
		return settings;
	}

}
