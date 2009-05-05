package harvester.processor.data.dao.interfaces;

import harvester.data.Harvest;

public interface HarvestDAO {

	public void ApplyChanges(Harvest h) throws Exception;
	
	public void AddToDatabase(Harvest h) throws Exception;
	
	public void makeLastSuccHarvest(int hid, int cid, int type) throws Exception;
	
}
