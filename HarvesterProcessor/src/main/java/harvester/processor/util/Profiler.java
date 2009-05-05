package harvester.processor.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import harvester.data.*;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.main.*;
import harvester.processor.steps.StagePluginInterface;
import harvester.processor.task.TaskProcessor;

import org.apache.log4j.Logger;

/**
 * Contains all logic related to the extraction of steps and there properties from the database.
 */
public class Profiler {

	private static Logger logger = Logger.getLogger(Profiler.class);
	
	/**
	 * Builds a list of StagePluginInterfaces based off the active profile and the harvest and load stages for this harvest.
	 * Should be called within a transaction.
	 * @param tp the Task processor object contains the harvest, contributor and collection objects needed to build the list.
	 * @return list of plugins for stages to run.
	 * @throws Exception
	 */
	public static LinkedList<StagePluginInterface> getStepObjects(TaskProcessor tp) throws Exception
	{
		logger.info("getting step objects");
		
		LinkedList<StagePluginInterface> l = new LinkedList<StagePluginInterface>();

		StepLogger slog = new StepLoggerImpl(tp.getH().getHarvestid(), tp.getClienturl());
		
		//first get the harvest step since it is stored seperately with the contributor object
		ProfileStep hstage = tp.getC().getHarveststage();
		if(hstage != null) {
			l.add(getInitialisedPlugin(hstage,slog, tp));	//nulls probably only exist here in testing, maybe
			l.getLast().setPosition(0);
		} else {
			logger.error("no harvest stage");
		}
		
		//get main pipeline stages, sorted by position
		TreeSet<ProfileStep> plss = new TreeSet<ProfileStep>(new pipelinestageComparator());
			
		if(tp.getDp() != null)
			plss.addAll(tp.getDp().getProfilesteps());
		
		for( ProfileStep ps : plss) {
			if(ps.getEnabled() == ProfileStep.ENABLED)
				l.add(getInitialisedPlugin(ps, slog, tp));
		}

		//get the loader step, which is stored with in collections class
		//get the parent collection from the contributor
		Collection collection = tp.getC().getCollection();
		ProfileStep lstage = collection.getLoadstage();
		if(lstage != null) {
			int position = l.getLast().getPosition();
			l.add(getInitialisedPlugin(lstage, slog, tp));
			l.getLast().setPosition(position + 1);
		} else {
			logger.error("no load stage");
		}
		
		return l;
	}

	/**
	 * creates the stage plugin interfaces object for the specified profile step
	 * @param ps profile step to create the stage for
	 * @param slog a logging object
	 * @param tp the taskprocessor containing most of the current state.
	 * @return the newly created stage plugin.
	 * @throws Exception
	 */
	private static StagePluginInterface getInitialisedPlugin(ProfileStep ps, StepLogger slog, TaskProcessor tp) throws Exception
	{
		Step s = ps.getStep();
		logger.info("Processing step: " + s.getName() + " position: " + ps.getPosition());

		//get the reflection object that allows us to examine and interact with the class
		Class c = Class.forName(s.getClassname());
		StagePluginInterface spi = (StagePluginInterface) c.newInstance();

		spi.setPosition(ps.getPosition());
		
		HashMap<String, Object> props = addProperties(ps, tp);

		//Initialise the step and add return it
		spi.Initialise(props,slog, tp.getServletCtx());
		
		logger.info("got step");
		return spi;
	}
	
	/**
	 * Extracts properties for the given profile step for this harvest from the database, and fills a map with them
	 * @param ps profile step data object
	 * @param tp task processor object
	 * @return properties map
	 */
	private static HashMap<String, Object> addProperties(ProfileStep ps, TaskProcessor tp) {
		
					//build the properties object to pass to its initialiser
					HashMap<String, Object> props = new HashMap<String, Object>();
					Set<ProfileStepParameter> params = ps.getParameters();
		
					//pass along application properties first,
					//that way database properties will override them if needed
					for(Object key : tp.getProps().keySet())
					{
						if( ((String)key).startsWith("all") || ((String)key).startsWith(ps.getStep().getClassname())  )
						{
							String propname = (String)key;
							//propname =  propname.substring(propname.indexOf(".")+1);
							propname =  propname.substring(propname.lastIndexOf(".")+1);
							logger.info("app property full=" + key  + " p=" + propname + " value=" + tp.getProps().getProperty((String)key));
							props.put(propname, tp.getProps().getProperty((String)key));
						}
					}
					
					//any files that are linked to this step
					List<StepFile> files = DAOFactory.getDAOFactory().getStepFileDAO().getFiles(ps.getStep().getStepid());
					props.put("stepFiles", files);
					
					//harvest related properties that are taken from the harvest object
					props.put("harvestid", String.valueOf(tp.getH().getHarvestid()));
					if(tp.getH().getHarvestfrom() != null)
						props.put("harvestfrom", tp.getH().getHarvestfrom());
					if(tp.getH().getHarvestuntil() != null)
						props.put("harvestuntil", tp.getH().getHarvestuntil());
					props.put("type", String.valueOf(tp.getH().getType()));
					if(tp.getDelete() != null)
						props.put("delete", tp.getDelete());
					if(tp.getUntil50() != null)
						props.put("until50", tp.getUntil50());
					if(tp.getSinglerecord() != null)
						props.put("singlerecord", tp.getSinglerecord());
						props.put("contributor", tp.getC());
					
					props.put("stage", String.valueOf(ps.getPosition()));
					props.put("stepid", ps.getStep().getStepid());
					
					//best way to do this might be to loop through things twice		
					//we first create any linked lists of hashtables that are needed 
					for(ProfileStepParameter p : params)
					{			
						logger.info("pname= " + p.getPis().getParametername() + " p=" + p.getProfilestepparameterid() + " value=" + p.getValue() + " parentid=" + p.getPis().getParentpiid());
						if(p.getPis().getParentpiid() == null)
						{
							if(p.getPis().getType().equals("nested"))
							{
								//build a list of hashtables 
								LinkedList<HashMap<String, String>> nestedparams = new LinkedList<HashMap<String,String>>();

								props.put(p.getPis().getParametername(), nestedparams);
								props.put(String.valueOf(p.getPis().getPiid()), nestedparams);
							} else
							{
								//you can't add a null to a properties object, for a very good reason
								//which I won't discuss here.
								if(p.getValue() != null)
									props.put(p.getPis().getParametername(), p.getValue());
							}
						}
					}
					
					//now we fill the created lists with all the passed nested properties
					//parameters are in ascending order by grouplistindex
					for(ProfileStepParameter p : params)
					{
						if(p.getPis().getParentpiid() != null)
						{
							//get the hashtable to add to 
							LinkedList<HashMap<String, String>> nestedparams = 
								(LinkedList<HashMap<String, String>>) props.get(p.getPis().getParentpiid().toString());
							
							//check to see if we need to add another hash to the list, or just add onto the end
							if( !p.getGrouplistindex().equals(nestedparams.size()) )
								nestedparams.add(new HashMap<String, String>());
							
							logger.info("NESTED: pname=" + p.getPis().getParametername() + " p=" + p.getProfilestepparameterid() + " value=" + p.getValue() 
										+ " parentid=" + p.getPis().getParentpiid() + " size of nested list = " + nestedparams.size());	
							nestedparams.getLast().put(p.getPis().getParametername(), p.getValue());
							
						}
					}
					
		return props;
	}
}
