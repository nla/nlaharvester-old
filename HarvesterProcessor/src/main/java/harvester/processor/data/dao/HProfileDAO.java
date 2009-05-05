package harvester.processor.data.dao;

import harvester.data.Profile;
import harvester.data.ProfileStep;
import harvester.processor.data.dao.interfaces.ProfileDAO;
import harvester.processor.util.HibernateUtil;

public class HProfileDAO implements ProfileDAO {

	public void addPipelineStage(Profile dp, ProfileStep ps) {
		//ps.setProfile(dp);
		dp.getProfilesteps().add(ps);
	}

	public Profile getProfile(int dataprofileid) {
		return (Profile) HibernateUtil.getSessionFactory().getCurrentSession().load( Profile.class, dataprofileid);
	}

}
