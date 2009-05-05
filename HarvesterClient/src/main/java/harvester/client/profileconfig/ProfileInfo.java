package harvester.client.profileconfig;

import harvester.data.*;

/**
 * Holds information on a particular profile in the session in one of the profile wizards.
 * Basically just links the profile with a contributor or collection id.
 * This class is sparse because its manipulated rather directly by the SessionProfileHandler,
 * which is the only class using this. Its basically just a key for find a profile in a list/.
 */
public class ProfileInfo implements java.io.Serializable{

	private static final long serialVersionUID = 456983080009L;

//	/**url to redirect to after editing this profile is complete */
//	private String redirecturl;
//	/** holds the current state of changes */
//	private Profile dp;
//	private boolean production;
//	/**used when this is a default profile to keep track of what the cancel button should do */
//	private boolean keep;
//	/**in the case of a collection profile, holds the premodified profile so we can easily revert.*/
//	private Profile unmodifiedprofile;
//
//	
	
	private Profile profile;
	
	private Integer collectionid;
	
	private Integer contributorid;

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public Integer getCollectionid() {
		return collectionid;
	}

	public void setCollectionid(Integer collectionid) {
		this.collectionid = collectionid;
	}

	public Integer getContributorid() {
		return contributorid;
	}

	public void setContributorid(Integer contributorid) {
		this.contributorid = contributorid;
	}
	
	public String toString() {
		if(collectionid != null)
			return "collection #" + collectionid + " profile { " + profile.toString() + " }";
		else
			return  "contributor #" + contributorid + " profile { " + profile.toString() + " }";
	}
	
}
