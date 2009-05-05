package harvester.processor.data.dao;

import harvester.processor.data.dao.interfaces.*;

public class HibernateDAOFactory extends DAOFactory {

	public HarvestDAO getHarvestDAO() {
		return new HHarvestDAO();
	}

	@Override
	public ProfileDAO getprofileDAO() {
		return new HProfileDAO();
	}

	@Override
	public ContributorDAO getcontributorDAO() {
		return new HContributorDAO();
	}

	@Override
	public HarvestdataDAO getHarvestdataDAO() {
		return new HHarvestDataDAO();
	}

	@Override
	public HarvestlogDAO getHarvestlogDAO() {
		return new HHarvestLogDAO();
	}

	@Override
	public ParameteroptionDAO getParameteroptionDAO() {
		return new HParameterOptionDAO();
	}

	@Override
	public StepDAO getStepDAO() {
		return new HStepDAO();
	}

	@Override
	public HarvestClusterDAO getHarvestClusterDAO() {
		return new HHarvestClusterDAO();
	}

	@Override
	public StepFileDAO getStepFileDAO() {
		return new HStepFileDAO();
	}
	
}
