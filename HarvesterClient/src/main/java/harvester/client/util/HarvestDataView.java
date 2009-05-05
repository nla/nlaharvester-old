package harvester.client.util;


/**
 * This class is used to hold record data that is to be displayed by the ViewRecordsController
 * It is also referenced by the harvest DAO, and is the type returned for paging requests
 * We can not directly pass the hibernate object to the view since we first need to convert
 * the blob field to a string.
 */

public class HarvestDataView {
	private int harvestdataid;
	private String data;
	private int stage;
	
	public int getHarvestdataid() {
		return harvestdataid;
	}
	public void setHarvestdataid(int harvestdataid) {
		this.harvestdataid = harvestdataid;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public int getStage() {
		return stage;
	}
	public void setStage(int stage) {
		this.stage = stage;
	}
}
