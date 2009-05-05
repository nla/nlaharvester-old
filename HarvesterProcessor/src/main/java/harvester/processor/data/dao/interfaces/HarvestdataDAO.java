package harvester.processor.data.dao.interfaces;

import harvester.data.HarvestData;

import java.util.LinkedList;

public interface HarvestdataDAO {
	public void AddToDatabase(HarvestData hd) throws Exception;
	public void AddToDatabaseBulk(LinkedList<Object> records, int harvestid, int stage) throws Exception; 
}
