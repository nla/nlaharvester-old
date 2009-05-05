package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.StepDAO;
import harvester.data.ParameterInformation;
import harvester.data.Step;

import java.util.List;

import org.apache.commons.logging.*;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;


@SuppressWarnings("unchecked")
public class HStepDAO implements StepDAO {

	protected final Log logger = LogFactory.getLog(HContributorDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	@Transactional(readOnly=true)
	public List<Step> getInputSteps() {
		List steps = sf.getCurrentSession().createQuery("from Step where type=0").list();
		Hibernate.initialize(steps);
		return steps;
	}

	@Transactional(readOnly=true)
	public Step getStep(int stepid, boolean options, boolean parameterInfos) {
    	Step s = (Step)sf.getCurrentSession().load(Step.class, stepid);
    	Hibernate.initialize(s);
    	
    	if(parameterInfos)
        	for(ParameterInformation pi : s.getPis())
        		Hibernate.initialize(pi.getOptions());
    	
    	if(options) {
        	for(ParameterInformation pi : s.getPis())
        	{
        		Hibernate.initialize(pi.getOptions());
        		Hibernate.initialize(pi.getNested());
        		for(ParameterInformation npi : pi.getNested())
        			Hibernate.initialize(npi.getOptions());
        	}
    	}
    	
    	return s;
	}
	
	@Transactional(readOnly=true)
	public String getParameterValue(int parameterid) {
		Query q = sf.getCurrentSession().createQuery(
				"select value from profilestepparameter where profilestepparameterid = " + parameterid);
		Object result = q.uniqueResult();
		return result.toString();
	}
	
	@Transactional(readOnly=true)
	public Step getStep(int stepid) {
		return getStep(stepid, false, false);
	}
	
	@Transactional(readOnly=true)
	public List<Step> getAllSteps() {
		List steps = sf.getCurrentSession().createQuery("from Step s where type > 1 order by s.type desc, s.name asc").list();
		Hibernate.initialize(steps);
		return steps;
	}

}
