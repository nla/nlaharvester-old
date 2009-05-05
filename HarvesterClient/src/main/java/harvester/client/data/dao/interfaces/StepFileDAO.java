package harvester.client.data.dao.interfaces;

import java.util.List;

import harvester.data.Contributor;
import harvester.data.StepFile;

public interface StepFileDAO {

	public List<StepFile> getFiles(int stepid);
	public StepFile getFile(int fileid);
	public String getFileData(int fileid) throws Exception;	
	public void saveOrUpdateFile(StepFile file);
	public void deleteFile(int fileid);
	public List<String> getClashes(int fileid, int fileidid);
	public List<String> getCollectionClashes(int fileid, int fileidid);
	
}
