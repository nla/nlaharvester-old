package harvester.client.connconfig;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import harvester.client.data.dao.DAOFactory;
import harvester.client.util.*;
import harvester.data.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This method is used during a create contributor or a edit connection settings wizard.
 * It contains helper functions that assist with manipulating the database's profile and
 * parameter format.
 */
public class PUtil {
	
    protected final static Log logger = LogFactory.getLog(PUtil.class);
	
    /** Specifies a return type of piid keys */
    public static final int INTEGER_KEYS = 1;
    /** return keyed by name */
    public static final int STRING_KEYS = 2;
    
	/**
	 * converts the contributor's harvest stage's parameters into a map,
       from there normal database format.
	 * @param con The Contributor data object
	 * @return map of the contributor's harvest stage's parameters.
	 */
	public static Map<String, String> parametersToMap(Contributor con) {
		//we can start by converting the weird database/hibernate format the parameters
		//are stored in into a simple hashmap.
		Map<String, String> pmap = new HashMap<String,String>();
		logger.info("harvest stage psid=" + con.getHarveststage().getPsid() + " num params=" + con.getHarveststage().getParameters().size());
		for(ProfileStepParameter p : con.getHarveststage().getParameters())
		{
			String key = p.getPis().getParametername();
			String value = p.getValue();
			pmap.put(key, value);
			logger.info("key=" + key + " value=" + value);
		}
		return pmap;
	}


	
	/**
	 * 	 Converts a profilesstep's parameters into a list of key value pairs in the obvious way
	 Has no knowledge of parameter nesting that is used in the processing steps wizard, but 
	 not in the connection settings wizard.
	 * @param stage a profilestep data object
	 * @param keytype should the keys be the piid? then keytype=INTEGER_KEYS other the keys are the parameter name
	 * @return a list of keyvalue pairs of parameters
	 */
	public static List<KeyValue> getKeyValueList(ProfileStep stage, int keytype) {
        Set<ProfileStepParameter> params = stage.getParameters();
        List<KeyValue> kvs = new LinkedList<KeyValue>();
        if(keytype == INTEGER_KEYS)
        {
	        for(ProfileStepParameter p : params)
	        	kvs.add(new KeyValue(String.valueOf(p.getPis().getPiid()), p.getValue()));
        }
        else
        {
	        for(ProfileStepParameter p : params)
	        	kvs.add(new KeyValue(p.getPis().getParametername(), p.getValue()));
        }
		return kvs;
	}
	
	
	/**
	 * Converts a profilesstep's parameters into a list of key value pairs in the obvious way, keyed by parameter name
	 * @param stage profile step data object
	 * @return  a list of keyvalue pairs of parameters
	 */
	public static List<KeyValue> getKeyValueList(ProfileStep stage)
	{
		return getKeyValueList(stage, STRING_KEYS);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////
	// The drop down that appears on the edit contributor page and the first page of the 
	// connection settings wizard is filled by the following two methods.
	
	
	/**
	 * Fetches a list of input stages
	 * @param daofactory data access factory
	 * @return A list of input stage tuples, key is the name, value is the stepid
	 */
	public static LinkedList<KeyValue> getPossibleInputStages(DAOFactory daofactory)
	{
		//get the steps from the db
		List<Step> steps = daofactory.getStepDAO().getInputSteps();
		
		//convert steps to a simple keyvalue list
		LinkedList<KeyValue> kvs = new LinkedList<KeyValue>();
		for(Step s : steps)
			kvs.add(new KeyValue(s.getName(),String.valueOf(s.getStepid())));
		
		return kvs;
	}
	
	/**
	 * Fetches a list of input stages, possibly restricted by the active profiles for the passed collection
	 * @param daofactory data access factory
	 * @param c the collection that we should use to check what input stages are allowed
	 * @param ipc We only allow input stages that have a inputplugin setup, this object holds those links
	 * @return A list of input stage tuples, key is the name, value is the stepid
	 */
	public static LinkedList<KeyValue> getPossibleInputStages(DAOFactory daofactory, Collection c, InputPluginConfigurer ipc)
	{
		//get the steps from the db
		List<Step> steps = daofactory.getStepDAO().getInputSteps();
		Set<Profile> dps = daofactory.getCollectionDAO().getCollectionAndDependents(c.getCollectionid()).getProfiles();
		
		//convert steps to a simple keyvalue list
		LinkedList<KeyValue> kvs = new LinkedList<KeyValue>();
		for(Step s : steps)
		{
			boolean hasDP = false;
			//check that this steps htype is the same as one of the default profile's htypes
			for(Profile dp : dps)
				if(ipc.getProcessors().get(s.getClassname()) != null && dp.getType() == ipc.getProcessors().get(s.getClassname()).getHtype())
					hasDP = true;
				
			//if there is no possibly input stages, we should probably just default to OAI
			//TODO: should this reference OAI, or do we want a different behavior?
			if(hasDP || s.getName().equals("OAI"))
			{
				logger.info("adding a harvest type to the dropdown list");
				kvs.add(new KeyValue(s.getName(),String.valueOf(s.getStepid())));
			}
		}
		
		
		return kvs;
	}
}
