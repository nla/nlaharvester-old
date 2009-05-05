package harvester.client.connconfig.actions;

import harvester.client.connconfig.StepParameterView;
import harvester.client.data.dao.DAOFactory;
import harvester.data.Contributor;

import java.util.List;
import java.util.Map;



/**
 * responsible for fetching harvest type specific data such as the names of the sets in a oai repository
 * always ran between the 2nd and third steps of the connection settings wizard.
 */
public interface StepOptionProcessor {

	//we also pass along a dao factory in case datebase access is needed, but only reads should be made!
	/**
	 * do any processing of the step parameter view passed in.
	 * @param c contributor that can be used if the details are needed
	 * @param daofactory used for data access
	 * @return success/failure
	 */
	public boolean process(List<StepParameterView> spv, DAOFactory daofactory, Contributor c);
	
	public void postProcess(Map<Integer, String> parameters, DAOFactory daofactory, Contributor c);
	
	//just a name
	public int getHtype();
	
}
