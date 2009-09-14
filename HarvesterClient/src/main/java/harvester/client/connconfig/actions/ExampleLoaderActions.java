package harvester.client.connconfig.actions;

import harvester.client.util.KeyValue;
import harvester.data.ProfileStep;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExampleLoaderActions implements LoadStepActions {

	private static Log logger = LogFactory.getLog(ArrowLoaderActions.class);
	
	public void deleteProductionRecords(int contributorid, String contributorname) {
		//Do whatever you need to here to delete the records
		logger.info("delete production records called");
	}

	public Integer getCollectionSize() {
		//Do whatever you need to to get the count of records in the production service
		return 0;
	}

	public List<KeyValue> getSettings(ProfileStep load_step) {
		//null is a valid return value
		return null;
	}

}
