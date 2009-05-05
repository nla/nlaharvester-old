package harvester.client.profileconfig;

import harvester.client.connconfig.*;
import harvester.client.util.*;
import harvester.data.*;

import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * General utility functions related to the processing of profiles for use in the profile wizard.
 */
@SuppressWarnings("static-access")
public class ProfileConfigUtil {
	
    private static Log logger = LogFactory.getLog(ProfileConfigUtil.class);
    
    /**
     * converts from the databases representation of a profile to the views representation
     * @param dp profile data object
     * @return list of stages in the profile for use by the view
     */
	public static List<Stage> getProfileView(Profile dp)
	{
		if(dp == null)
			return null;
		//we basically just convert between the profilestep representation
		//to a list of stages
		LinkedList<Stage> stages = new LinkedList<Stage>();
		
		//we need to sort the set before we do anything
		//we use a treeset for this, its fairly efficient (O(ln n) adds)
		TreeSet<ProfileStep> ts = new TreeSet<ProfileStep>(new PipelineStageComparator());
		ts.addAll(dp.getProfilesteps());
		
		logger.info("converting pipelinestage set to a stage list");
		for(ProfileStep ps : ts) 
		{
			Stage s = new Stage();
			s.setPosition(ps.getPosition());
			s.setPsid(ps.getPsid());
			//s.setIsreadonly(ps.getIsreadonly() == null ? false : ps.getIsreadonly());
			s.setEnabled(ps.getEnabled());
			s.setRestriction(ps.getRestriction());
			s.setStepid(ps.getStep().getStepid());
			s.setDescription(ps.getStep().getName());
			s.setInput(ps.getStep().getInputtype());
			s.setOutput(ps.getStep().getOutputtype());
			s.setFunctiondescription(ps.getDescription());
			stages.add(s);
			logger.info("stage id=" + s.getPsid() + " position=" + s.getPosition() + " desc=" + s.getDescription());
		}
		
		return stages;	
	}
	
	/**
	 * 	the edit processing step page uses this function to convert a profile step into the views representation, which is
		just a list of step parameter views.
	 * @param ps profile step data object
	 * @return list of step parameter views.
	 */
	public static List<StepParameterView> getParameterView(ProfileStep ps)
	{		
		logger.info("step=" + ps.getStep());
		LinkedList<StepParameterView> spvs = new LinkedList<StepParameterView>();	
		HashMap<Integer, ProfileStepParameter> params = getParameterMap(ps);	//extract parameter values into a map
		
		for(ParameterInformation pi : ps.getStep().getPis())
		{
			StepParameterView spv = convertToView(pi, params, ps);
			if(spv != null)
			{
				ProfileStepParameter p = params.get(pi.getPiid());
				if( p != null)			
					spv.setValue(EscapeHTML.forHTML(p.getValue()));
					
				logger.info("adding spv name=" + spv.getName() + " value=" + spv.getValue() +  " type=" + spv.getType());
				spvs.add(spv);
			}
		}
		
		//log what this data structure looks like
		for(StepParameterView spv : spvs)
			logger.info(spv.toString("  "));
		return spvs;
	}

	/**
	 * recursive function for generating step parameter views that map have nested parameters.
	 * @param pi parameter information data  object
	 * @param params map of parameter values
	 * @param ps profile step data object
	 * @return Step parameter view
	 */
	private static StepParameterView convertToView(ParameterInformation pi, HashMap<Integer, ProfileStepParameter> params, ProfileStep ps) {
		
		StepParameterView spv = new StepParameterView();
		spv.setDescription(pi.getDescription());
		spv.setId(pi.getPiid());
		spv.setName(pi.getParametername());
		spv.setEditibility(pi.getEditibility());
		
		//readonly fields can only be text
		if(pi.getEditibility() == 2 && (pi.getOptions() == null || pi.getOptions().size() == 0) )
			spv.setType(spv.READ_ONLY);
		else
		{
			if(pi.getOptions() != null && pi.getOptions().size() > 0)
			{
				///////////////////MULTIPLE CHOICE
				spv.setOptions(new LinkedList<KeyValue>());
				
				if(pi.getEditibility() == 2)
					spv.setType(spv.RADIO);
				else
					spv.setType(spv.DROP_DOWN);
				
				for(ParameterOption po : pi.getOptions())
					spv.getOptions().add(new KeyValue(EscapeHTML.forHTML(po.getValue()), po.getDescription()));
			} else if(pi.getType().equals("Boolean"))
			{
				//////////////////CHECK BOX
				logger.info("setting type to check box");
				spv.setType(spv.CHECK_BOX);
			} else if(pi.getType().equals("nested"))
			{
				/////////////////NESTED PARAMTER 
				logger.info("setting type to nested");
				
				spv.setNested(new LinkedList<StepParameterView>());
				
				ProfileStepParameter psp = params.get(pi.getPiid());
				if(psp != null && psp.getValue() != null && !psp.getValue().equals(""))
					spv.setNumberofnested(Integer.valueOf(psp.getValue()));
				else
					spv.setNumberofnested(1);
				
				//Add the nested parameters on recursively
				for(ParameterInformation npi : pi.getNested())
					spv.getNested().add(convertToView(npi, params, ps));
				
				//the values for a nested paraemeter are stored differently then a normal parameter
				//so I use a different method to build the parameter map.
				//in fact they are stored in a map as well, which is very different from a regular paremeter
				spv.setNestedvalues(createNestedParameterMap(ps, pi.getPiid()));
				
				spv.setType(spv.NESTED);
			} else
			{
				////////////default to TEXT
				spv.setType(spv.TEXT);
				if(pi.getType().equals("regex"))
					spv.setSubtype(spv.REGEX);
				if(pi.getType().equals("xpath"))
					spv.setSubtype(spv.XPATH);
			}
		}
		return spv;
	}

	/**
	 * of the three map building functions, this one is a merger of the other two. It is only used when updating parameters,
	 where it is convenient to have all the values in the same map.
	 the keys are of the following format  "piid" if it is a non-nested parameter, "piid.child-piid.groupindex" for all the
	 children of piid. note that it does not support more then one level of nesting.
	 see stepparameterview for description of group index.
	 * @param ps profile step data object
	 * @return map of parameter values.
	 */
	private static HashMap<String, Integer> getParameterMapWithNested(ProfileStep ps) {
		//its easiest if we put the existing parameters into a hashmap
		HashMap<String, Integer> params = new HashMap<String, Integer>();
		for(ProfileStepParameter p : ps.getParameters())
		{			
			logger.info("parameter pid=" + p.getProfilestepparameterid() + " value=" + p.getValue());
			if(p.getPis().getParentpiid() == null)
				params.put(p.getPis().getPiid().toString(), p.getProfilestepparameterid());
		}
		for(ProfileStepParameter np : ps.getParameters())
		{
			if(np.getPis().getParentpiid() != null)
			{
				Integer parentid = params.get(np.getPis().getParentpiid().toString());
				if(parentid != null)
				{
					String key = parentid + "." + np.getPis().getPiid() + "." + np.getGrouplistindex();
					params.put(key, np.getProfilestepparameterid());
					logger.info("added key=" + key + " value=" + np.getProfilestepparameterid());
				}
			}
		}
		return params;
	}
	
	/**
	 * used for building a view of parameters. Its convienient to have the parameter values in a map since
	 we need random access. Keyed by piid(parameter info id).
	 * @param ps profile step data object
	 * @return map of parameter values.
	 */
	private static HashMap<Integer, ProfileStepParameter> getParameterMap(ProfileStep ps) {
		//its easyiest if we put the existing parameters into a hashmap
		HashMap<Integer, ProfileStepParameter> params = new HashMap<Integer, ProfileStepParameter>();
		for(ProfileStepParameter p : ps.getParameters())
		{			
			if(p.getPis().getParentpiid() == null)
			{
				logger.info("parameter pid=" + p.getProfilestepparameterid() + " value=" + p.getValue());
				params.put(p.getPis().getPiid(), p);
			}

		}
		return params;
	}
	
	/**
	 * 	builds a map of the values of parameters, for easy access. the keys are a string of the following format
	 "piid.groupindex" where piid is the parameter and group index is as explained in stepparameterview class.
	 * @param ps profile step data object
	 * @param piid parameter info id
	 * @return parameter value map
	 */
	private static HashMap<String, String> createNestedParameterMap(ProfileStep ps, Integer piid)
	{
		logger.info("creating nested parameter map with parent piid" + piid + " num of params=" + ps.getParameters().size());
		HashMap<String, String> params = new HashMap<String, String>();
		
		//retrieve all nestedprofilestepparameter objects linked by the nested table
		//then foreach one of them, add to the hashmap
		for(ProfileStepParameter np : ps.getParameters())
		{
			logger.info("checking piid=" + np.getPis().getPiid() + " value=" + np.getValue() + " parentpiid=" + np.getPis().getParentpiid());
			if(piid.equals(np.getPis().getParentpiid()))
			{
				String str = np.getPis().getPiid() + "." + np.getGrouplistindex();
				params.put(str, EscapeHTML.forHTML(np.getValue()));
				logger.info("nested " + str + " value=" + np.getValue());
			}
		}
		
		return params;
	}

	/**
	 *  called by the modify page to make the changes to the profilestep object contained within the pmap.
	 * @param ps profile step data object
	 * @param pmap parameter value map
	 */
	public static void updateparameters(ProfileStep ps, Map pmap) {
		
		//we will just create a new hashset to replace the old parameter set
		//this will cause more updates then needed unless parameter implements
		//equals and hash properly as specified in the hibernate docs. It might already
		HashSet<ProfileStepParameter> params = new HashSet<ProfileStepParameter>();
		
		HashMap<String, Integer> oldparams = getParameterMapWithNested(ps);
		
		//we need to clear out nested parameters from the parameter set
		logger.info("clearing out nested parameters, total paramters currently" + ps.getParameters().size());
		for(Iterator<ProfileStepParameter> i = ps.getParameters().iterator(); i.hasNext() ;)
		{
			ProfileStepParameter psp = i.next();
			if(psp.getPis().getParentpiid() != null)
				i.remove();
		}
		logger.info("finished clearing, now have " + ps.getParameters().size());
		
		for(ParameterInformation pi : ps.getStep().getPis())
		{
			Object ovalue = pmap.get(String.valueOf(pi.getPiid()));
			String value = null;
			if(ovalue != null)
				value = ovalue.toString();
			
			logger.info("adding parameter pi=" + pi.getPiid() + " vkey=" + pi.getParametername());
			ProfileStepParameter p = new ProfileStepParameter();
			p.setPis(pi);
			p.setPss(ps);
			p.setGrouplistindex(1);
			
			Integer oldp = oldparams.get(pi.getPiid().toString());
			
			if(pi.getType().equals("nested"))
			{
				//we need to also create the nested ones
				//loop through all returned request objects to find the ones that are part of this nesting
				Set names = pmap.keySet();
				int rowceiling = 0;
				for(Object name : names)
				{
					logger.info("testing name=" + name);
					if(name.toString().startsWith(pi.getPiid().toString() + "."))
					{						
						//name is of the correct form to be nested beneath this pi
						String[] splitname = name.toString().split("\\.");
						logger.info("name= " + name + " split into " + splitname.length + "pieces");

						HashMap<Integer, ParameterInformation> nestedpis = new HashMap<Integer, ParameterInformation>();
						for(ParameterInformation npi : pi.getNested())
							nestedpis.put(npi.getPiid(), npi);
						
						int rownum = Integer.valueOf(splitname[2]);
						if(rownum > rowceiling)
							rowceiling = rownum;
						
						ProfileStepParameter np = new ProfileStepParameter();
						np.setPis(nestedpis.get(Integer.valueOf(splitname[1])));
						np.setGrouplistindex(Integer.valueOf(splitname[2]));
						np.setValue((String)pmap.get((String) name));
						np.setPss(p.getPss());
						
						//if this is not a new one they added, we want to use the old id so we don't have to create a new row
						//when we save to the database
						Integer oldid = oldparams.get(name);
						if(oldid != null)
						{
							logger.info("found old npid=" + oldid);
							np.setProfilestepparameterid(oldid);
						}
						
						params.add(np);
						logger.info("added new parameter for nesting, value=" + np.getValue() + " pid=" + np.getProfilestepparameterid());
					}
				}
				p.setValue(String.valueOf(rowceiling));

			} else
			{
				p.setValue(value);
			}
			
			if(oldp != null)
				p.setProfilestepparameterid(oldp);
			params.add(p);
		}
		
		ps.getParameters().clear();
		ps.getParameters().addAll(params);
		
		//if they deleted any rows there will be holes in the row numbering that need to be fixed
		for(ParameterInformation pi : ps.getStep().getPis())
			if(pi.getType().equals("nested"))
				fixRowNumbers(pi, ps);
		
		
	}
	
	/**
	 * Fetches the default profiles for the passed contributor
	 * @param c contributor
	 * @return A list of tuples, of the form (profileid, profilename)
	 */
	public static List<KeyValue> getDefaultDataprofiles(Contributor c)
	{
		Set<Profile> dps = c.getCollection().getProfiles();
		//create keyvalue representation
		LinkedList<KeyValue> kvs = new LinkedList<KeyValue>();
		for(Profile dp : dps)
		{
			if(dp.getType() == c.getHtype())
			{
				KeyValue kv = new KeyValue();
				kv.setKey(dp.getProfileid().toString());
				kv.setValue(dp.getName());
				kvs.add(kv);
				logger.info("got default dataprofile id= " + kv.getKey() + " name=" + kv.getValue());
			}
		}
		
		return kvs;
	}
	
	/**
	 * 	 when removing a profilestep from a profile we need to change the positions of the other profilesteps
	 so that there are not any holes in the numbering.
	 * @param dp profile data object
	 * @param position of the profile step to remove.
	 */
	public static void removeFromPipeline(Profile dp, int position)
	{
		logger.info("deleting stage, position=" + position);
		
		ProfileStep removeme = null;	//we can't remove it while looping over the list, so do it after
		for(ProfileStep ps : dp.getProfilesteps()) //iterator not as nice here
		{
			if(ps.getPosition() == position)
			{
				removeme = ps;			
			} else if(ps.getPosition() > position)	//we need to move everything else with higher positions down one
			{
				ps.setPosition(ps.getPosition()-1);
			}
		}
		dp.getProfilesteps().remove(removeme);	
	}
	
	/**
	 * 	as above, when adding a profile step to a profile we need to make sure we don't inadvertently introduce holes in
	the position numbering.
	 * @param dp profile data object
	 * @param newps profilestep object to add, in the position specified in the object.
	 */
	public static void addToPipeline(Profile dp, ProfileStep newps)
	{
		logger.info("adding pipelinestage into position " + newps.getPosition());
		int biggest = 1;
		if(newps.getPosition() < 1)
			newps.setPosition(1);	//make sure the number is realistic
		
		for(ProfileStep ps : dp.getProfilesteps())
		{
			if(ps.getPosition() > biggest)
			{
				biggest = ps.getPosition();
			}
			if(ps.getPosition() >= newps.getPosition())	//we need to move everything else with higher positions up one
			{
				ps.setPosition(ps.getPosition()+1);
			}
		}
		//we have to make sure we aren't adding a pipeline stage above the greatest in the pipeline
		if(newps.getPosition() > biggest)
		{
			logger.info("bigger then biggest");
			newps.setPosition(biggest+1);
		}
		logger.info("adding into position, position=" + newps.getPosition() + " biggest=" + biggest);
		dp.getProfilesteps().add(newps);
	}
	
	/**
	 * moves a profilestep around in a profile, keeping the position numbering consistant and hole free
	 * @param dp profile data object
	 * @param ps profile step data object to move
	 * @param newposition the position to move it to.
	 */
	public static void changePosition(Profile dp, ProfileStep ps, int newposition)
	{
		logger.info("changing position of profilestep psid=" + ps.getPsid() + " step=" + ps.getStep().getName() + " from " + ps.getPosition() + " to " + newposition);
		removeFromPipeline(dp, ps.getPosition());
		ps.setPosition(newposition);
		addToPipeline(dp, ps);
	}

	/**
	 * it is important that the group indexes don't have any holes in the numbering, so after we make changes to a profilestep
	 using what the user has entered we also need to make adjustments to the groupindexes.
	 * @param pi parameter information data object for the parent parameter of the nested parameters to fix 
	 * @param ps profile step data object
	 */
	public static void fixRowNumbers(ParameterInformation pi, ProfileStep ps)
	{
		//basically we need to sort the nested p's by row number then loop over them,  reducing row numbers to fill gaps
		//create a set sorted by rownumber
		Set<ProfileStepParameter> sortednps = new TreeSet<ProfileStepParameter>(new ParameterComparator());
		
		ProfileStepParameter p = null;
		
		//add them all to this new set
		for(ProfileStepParameter psp : ps.getParameters())
		{
			if(pi.getPiid().equals(psp.getPis().getParentpiid()))
				sortednps.add(psp);
			else if(psp.getPis().getPiid().equals(pi.getPiid()))
				p = psp;
		}
		
		int offset = 0; //hold the count of the number of rows of 'holes' found in the list
		int current = 0;
		logger.info("fixing rownumbers, total paramters=" + ps.getParameters().size() + " size of sorted set is " + sortednps.size());
		for(ProfileStepParameter np : sortednps)
		{
			logger.info("before rownumber=" + np.getGrouplistindex() + " current="  + current + " offset=" + offset);
			if( np.getGrouplistindex() > current)
			{
				offset = offset + ( np.getGrouplistindex()-current-1);	//this should only change the offset if there is a missing row or several
				current =  np.getGrouplistindex();
			}

			np.setGrouplistindex(current-offset);
			logger.info("after rownumber=" +  np.getGrouplistindex() + " current="  + current + " offset=" + offset);
		}
		String pvalue = String.valueOf(current-offset);
		logger.info("setting p value to " + pvalue);
		p.setValue(pvalue);
	}
	
}
