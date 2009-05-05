package harvester.client.connconfig.actions;

/**
 * This class provides an interface that must be implemented for all datastores that
 * Are availible in the views. It simply provides the delete records facility.
 */
public interface LoadStepActions {

	public void deleteProductionRecords(int contributorid, String contributorname);
	public Integer getCollectionSize();
	
}
