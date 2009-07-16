package harvester.client.connconfig.actions;

import harvester.client.connconfig.StepParameterView;
import harvester.client.data.dao.DAOFactory;
import harvester.data.Contributor;

import java.util.List;
import java.util.Map;

public class Z3950OptionProcessor implements StepOptionProcessor {

	public int getHtype() {
		return 1;
	}

	public void postProcess(Map<Integer, String> parameters,
			DAOFactory daofactory, Contributor c) {

	}

	public boolean process(List<StepParameterView> spv, DAOFactory daofactory,
			Contributor c) {
		return true;
	}

}
