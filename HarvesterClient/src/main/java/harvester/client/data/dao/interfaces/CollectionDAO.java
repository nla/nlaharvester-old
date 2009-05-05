package harvester.client.data.dao.interfaces;

import java.util.LinkedList;

import java.util.List;
import harvester.client.util.KeyValue;
import harvester.data.*;


public interface CollectionDAO {

	public Collection getCollection(int collectionid);
	public List<Collection> getAllCollections();
	public Collection getCollectionAndDependents(int collectionid);
	public Collection getCollectionContributorsAndLastHarvests(int collectionid);
	
	public Collection saveOrUpdateCollection(Collection c, Integer psid);
	
	public List<ProfileStep> getPossibleOutputStages();
	public boolean isNameInUse(String name);
	
	public LinkedList<KeyValue> getDefaultProfilesForCollection(int collectionid, int htype);
	public int getContributorCount(int collectionid);
	public void deleteCollection(int collectionid);
	public Collection getCollectionAndContacts(int collectionid);
	public Collection getCollectionAndProfiles(int collectionid);
	public void modifyCollection(Collection c);
	
}
