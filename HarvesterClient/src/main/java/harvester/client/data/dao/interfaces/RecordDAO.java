package harvester.client.data.dao.interfaces;

import java.util.Date;
import java.util.List;

public interface RecordDAO {
	
	public String getHarvestLogRecordData(int harvestlogid) throws Exception;
	public String getHarvestDataRecordData(int harvestdataid) throws Exception;
	
	public List<Object> getHarvestLogs(int harvestid);
	public List<Object> getNewHarvestLogs(int harvestid, Date fromdate);
	public List<Object> getVisibleHarvestLogsInRange(int harvestid, int start, int end);
}
