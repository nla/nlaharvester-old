package harvester.client.data.dao.interfaces;

import java.util.List;

public interface RecordDAO {
	
	public String getHarvestLogRecordData(int harvestlogid) throws Exception;
	public String getHarvestDataRecordData(int harvestdataid) throws Exception;
	
	public List<Object> getHarvestLogs(int harvestid);
	
}
