package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.*;

public abstract class DAOFactory {
	
	public abstract HarvestDAO getHarvestDAO();
	public abstract ProfileDAO getProfileDAO();
	public abstract ContributorDAO getContributorDAO();
	public abstract CollectionDAO getCollectionDAO();
	public abstract NoteDAO getNoteDAO();
	public abstract StepDAO getStepDAO();
	public abstract RecordDAO getRecordDAO();
	public abstract StepFileDAO getStepFileDAO();
	public abstract ReportDAO getReportDAO();
	
}
