package harvester.processor.data.dao;

import harvester.processor.data.dao.interfaces.*;

public abstract class DAOFactory {
	
	public abstract HarvestDAO getHarvestDAO();
	public abstract ProfileDAO getprofileDAO();
	public abstract ContributorDAO getcontributorDAO();
	public abstract HarvestdataDAO getHarvestdataDAO();
	public abstract HarvestlogDAO getHarvestlogDAO();
	public abstract ParameteroptionDAO getParameteroptionDAO();
	public abstract StepDAO getStepDAO();
	public abstract HarvestClusterDAO getHarvestClusterDAO();
	public abstract StepFileDAO getStepFileDAO();
	
	public static DAOFactory getDAOFactory()
	{
		return new HibernateDAOFactory();
	}
}
