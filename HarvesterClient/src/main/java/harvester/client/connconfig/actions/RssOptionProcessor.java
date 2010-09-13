package harvester.client.connconfig.actions;

import harvester.client.connconfig.StepParameterView;
import harvester.client.data.dao.DAOFactory;
import harvester.data.Contributor;

import java.util.List;
import java.util.Map;

public class RssOptionProcessor implements StepOptionProcessor {

	public int getHtype() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void postProcess(Map<Integer, String> parameters,
			DAOFactory daofactory, Contributor c) {
		// TODO Auto-generated method stub

	}

	public boolean process(List<StepParameterView> spv, DAOFactory daofactory,
			Contributor c) {
		// TODO Auto-generated method stub
		return true;
	}

}
