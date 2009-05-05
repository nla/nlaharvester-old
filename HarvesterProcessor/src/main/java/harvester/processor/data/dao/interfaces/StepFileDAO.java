package harvester.processor.data.dao.interfaces;


import harvester.data.StepFile;
import java.util.List;

public interface StepFileDAO {
	public List<StepFile> getFiles(int stepid);
	public String getFileData(int fileid);
}
