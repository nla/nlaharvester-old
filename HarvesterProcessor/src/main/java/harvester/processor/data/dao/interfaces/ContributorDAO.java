package harvester.processor.data.dao.interfaces;

import harvester.data.Contributor;

import java.util.Date;

public interface ContributorDAO {
	
	public void setTotalRecords(int contributorid, int recordsincollection, int recordsfromcontributor) throws Exception;
	public Contributor getContributor(int contributorid);
	//public void setLastHarvestDate(Date lhdate, int contributorid) throws Exception;
}
