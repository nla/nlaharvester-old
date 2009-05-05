package harvester.client.profileconfig;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import harvester.data.*;

/**
 * holds all session information need while in a profile wizard. uses a session scoped bean.
 * All access to this is done through the ProfileSession class, this is basically a wrapper
 * that is session scoped rather then request scoped.
 * A profile can be either at collection level or contributor level, which is why we need 
 * to use a profile info object that is aware of this instead of just using a list of Profiles
 * directly.
 */
public class SessionProfileHandler implements java.io.Serializable {
	
	private static final long serialVersionUID = 112398321L;

	//this will not be a performance issue
	private List<ProfileInfo> profiles = new LinkedList<ProfileInfo>();
	
	private HashMap<Integer, Profile> tmpProfiles = new HashMap<Integer, Profile>();
	
	
	public HashMap<Integer, Profile> getTmpProfiles() {
		return tmpProfiles;
	}
	
	/**
	 * Retrieves from the session the profile specified by the given parameters, if one exists in the session.
	 * @param profileid	
	 * @param contributorid
	 * @return instance of a Profile
	 */
	public Profile getContributorProfile(int profileid, int contributorid) {
		
		for(ProfileInfo profileinfo : profiles) {
			if(profileinfo.getProfile().getProfileid().equals(profileid) && profileinfo.getContributorid().equals(contributorid))
				return profileinfo.getProfile();
		}
		return null;
	}
	/**
	 * Retrieves from the session the profile specified by the given parameters, if one exists in the session.
	 * @param profileid
	 * @param collectionid
	 * @return instance of a Profile
	 */
	public Profile getCollectionProfile(int profileid, int collectionid) {
		
		for(ProfileInfo profileinfo : profiles) {
			if(profileinfo.getProfile().getProfileid().equals(profileid) && profileinfo.getCollectionid().equals(collectionid))
				return profileinfo.getProfile();
		}
		return null;
	}
	
	/**
	 * remove the passed profile from the session if it exists in the session, otherwise leaves the session unchanged
	 * @param p
	 */
	public void remove(Profile p) {
		profiles.remove(p);
	}
	
	/**
	 * Adds the given profile to the session, at the contributor level.
	 * @param dp
	 * @param contributorid
	 */
	public void addContributorProfile(Profile dp, int contributorid) {

		//remove any old profiles that are the same
		for(java.util.Iterator<ProfileInfo> itor = profiles.iterator(); itor.hasNext();) {
			ProfileInfo profileinfo = itor.next();
			if(profileinfo.getProfile().getProfileid().equals(dp.getProfileid()) && profileinfo.getContributorid().equals(contributorid))
				itor.remove();
		}
		//add the new one
		ProfileInfo pi = new ProfileInfo();
		pi.setContributorid(contributorid);
		pi.setProfile(dp);
		profiles.add(pi);					
	}

	/**
	 * Adds the given profile to the session, at the collection level
	 * @param dp
	 * @param collectionid
	 */
	public void addCollectionProfile(Profile dp, int collectionid) {
		//remove any old profiles that are the same
		for(java.util.Iterator<ProfileInfo> itor = profiles.iterator(); itor.hasNext();) {
			ProfileInfo profileinfo = itor.next();
			if(profileinfo.getProfile().getProfileid().equals(dp.getProfileid()) && profileinfo.getCollectionid().equals(collectionid))
				itor.remove();
		}
		//add the new one
		ProfileInfo pi = new ProfileInfo();
		pi.setCollectionid(collectionid);
		pi.setProfile(dp);
		profiles.add(pi);	
	}
	
	/**
	 * prints this object in a user friendly way. Used for debugging.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("sessionprofile handler has #" + profiles.size() + " profiles\n");
		for(ProfileInfo pi : profiles)
			sb.append(pi.toString() + "\n");
		return sb.toString();
			
	}
}
