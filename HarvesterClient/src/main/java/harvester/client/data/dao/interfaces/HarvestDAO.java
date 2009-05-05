package harvester.client.data.dao.interfaces;
import harvester.client.harvest.HarvestError;
import harvester.client.util.HarvestDataView;
import harvester.data.*;

import java.util.List;

import javax.servlet.ServletOutputStream;


public interface HarvestDAO {

	public Harvest getHarvestContributorCollectionAndLogs(int harvestid);

	public Harvest getHarvestContributorCollectionAndData(Integer harvestid); 

	public List<HarvestDataView> getHarvestDataWithPaging(int page, int harvestid, int pagesize);

	public Harvest getHarvestAndContributor(Integer harvestid);
	
	public int getTotalDataRecords(int harvestid);

	public Harvest getHarvestContributorCollection(int harvestid);
	
	//public void setHidden(int harvestid);

	public void doHardHarvestStop(Integer valueOf);
	
	public void getStreamingHarvestData(int harvestid, ServletOutputStream os) throws Exception;
	
	public List<HarvestCluster> getClusters(int harvestid);
	
	public boolean hasClusters(int harvestid);
	
	public void deleterecords(Integer harvestid);
	
	public List<HarvestError> getErrorSummary(int harvestid);
	
}
