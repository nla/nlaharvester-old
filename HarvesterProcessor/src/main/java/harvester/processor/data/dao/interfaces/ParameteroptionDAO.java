package harvester.processor.data.dao.interfaces;

import harvester.data.ParameterOption;

import java.util.HashMap;

public interface ParameteroptionDAO {

	void updateOptions(HashMap<String, ParameterOption> pos, Integer piid) throws Exception;

	
}
