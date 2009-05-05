package harvester.processor.data.dao.interfaces;

import harvester.data.Profile;
import harvester.data.ProfileStep;

public interface ProfileDAO {
	
	//public void addPipelineStage(Profile dp, ProfileStep ps);
	
	public Profile getProfile(int profileid);
	
}
