package harvester.client.data.dao.interfaces;
import harvester.data.*;

public interface ProfileDAO {

	public Profile getProfile(int dataprofileid);
	
	public void saveProfile(Profile dp, int contributorid, boolean production);

	public Profile getProfileAndEvict(int profileid);
	public void saveCollectionProfile(Profile dp, int colectionid);
	public Contributor findCloneAndAttachDefaultProfile(Contributor c);
	public Contributor AttachDefaultProfile(int profileid, Contributor cf);

	public void deleteProfile(int profileid, int collectionid);
	public void deleteProfile(int profileid);
	
	public Profile copyProfile(Integer profileid, int contributorid, boolean overproduction);
	
}
