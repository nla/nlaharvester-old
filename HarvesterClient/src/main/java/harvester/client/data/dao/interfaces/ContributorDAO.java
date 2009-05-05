package harvester.client.data.dao.interfaces;
import harvester.client.util.KeyValue;
import harvester.data.*;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface ContributorDAO {
	
	public Contributor getContributor(int contributorid);
	public Contributor getContributorCollectionContactsAndLastHarvest(int contributorid);
	public Contributor getContributorAndHarvestStepDetails(int contributorid);
	public Contributor getContributorCollectionAndNotes(int contributorid);
	public Contributor getContributorCollectionAndHarvests(int contributorid);
	public Contributor getContributorCollectionAndDataprofile(int contributorid);
	public Contributor getContributorCollectionAndDetailedDataprofile(int contributorid);
	public Contributor getContributorLastHarvestsAndCollection(int contributorid);
	public Contributor getContributorAndConnectionSettings(int contributorid);
	public void updateContributor(Contributor c);
	public void saveOrUpdateContributor(Contributor c);
	public void addNewHarvestStage(int cid, int stepid, Map<Integer, String> parameters);
	public void togglemonitored(int contributorid);
	public void toggleproduction(int contributorid);
	public void deletecontributor(int contributorid);
	public void doRemoveRecords(int contributorid, int type);
	public void setHidden(Integer contributorid);
	public void setRecordCount(int contributorid, int count);
	public Date getLastProductionHarvestDate(int contributorid);
	public boolean isNameInUse(String name, String collectionid);
}
