package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.*;
import org.apache.commons.logging.*;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DAOFactoryImpl extends DAOFactory {

    protected final Log logger = LogFactory.getLog(DAOFactoryImpl.class);
	
    private HarvestDAO harvestDAO;
    private ProfileDAO profileDAO;
    private ContributorDAO contributorDAO;
    private CollectionDAO collectionDAO;
    private NoteDAO noteDAO;
    private StepDAO stepDAO;
    private RecordDAO recordDAO;
    private StepFileDAO stepFileDAO;
    private ReportDAO reportDAO;
    
	public ReportDAO getReportDAO() {
		return reportDAO;
	}
	public void setReportDAO(ReportDAO reportDAO) {
		this.reportDAO = reportDAO;
	}
	public StepFileDAO getStepFileDAO() {
		return stepFileDAO;
	}
	public void setStepFileDAO(StepFileDAO stepFileDAO) {
		this.stepFileDAO = stepFileDAO;
	}
	public RecordDAO getRecordDAO() {
		return recordDAO;
	}
	public void setRecordDAO(RecordDAO recordDAO) {
		this.recordDAO = recordDAO;
	}
	public StepDAO getStepDAO() {
		return stepDAO;
	}
	public void setStepDAO(StepDAO stepDAO) {
		this.stepDAO = stepDAO;
	}
	public HarvestDAO getHarvestDAO() {
		return harvestDAO;
	}
	public void setHarvestDAO(HarvestDAO harvestDAO) {
		this.harvestDAO = harvestDAO;
	}
	public NoteDAO getNoteDAO() {
		return noteDAO;
	}
	public void setNoteDAO(NoteDAO noteDAO) {
		this.noteDAO = noteDAO;
	}
	public CollectionDAO getCollectionDAO() {
		return collectionDAO;
	}
	
	public ProfileDAO getProfileDAO() {
		return profileDAO;
	}
	public void setProfileDAO(ProfileDAO dataprofileDAO) {
		this.profileDAO = dataprofileDAO;
	}
	public ContributorDAO getContributorDAO() {
		return contributorDAO;
	}
	public void setContributorDAO(ContributorDAO contributorDAO) {
		this.contributorDAO = contributorDAO;
	}

	public void setCollectionDAO(CollectionDAO collectionDAO) {
		this.collectionDAO = collectionDAO;
	}
}
