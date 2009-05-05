package harvester.processor.data.dao.interfaces;

import java.util.List;

import harvester.data.HarvestLog;
import harvester.processor.email.HarvestError;

public interface HarvestlogDAO {
	public void AddToDatabase(HarvestLog hl);
	public List<HarvestError> getErrorSummary(int harvestid);
}
