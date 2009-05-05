package harvester.client.profileconfig;

import java.util.Map;

/** Objects implementing this interface are used to do any required customized behavior
 * For a edit processing step screen(in tandom with a velocity view.
 * These classes are associated with the stepid of the step they are needed for in the 
 * servletcontext xml document.
 * 
 * Basically, preprocess is called before the view is constructed as postprocess after.
 * Preprocess is responsible for making any changes to the standard model object that 
 * would normally be generated for a step. It is called after the model object has been
 * filled.
 * 
 * PostProcess is called with the map of values essentially straight out of the servlet's
 * response. Any files that where returned will have been handled magically before this
 * is called, and just converted to strings in this map, in the obvious way.
 * if no action is needed, it should still contain something like the  following:
 * 		Map newmap = new HashMap();
 *		newmap.putAll(inmap);
 *		
 *		return newmap;
 */
public interface ICustomizedStep {
	
	/** 
	 * Processes the model before it is passed to the view
	 * @param model the model object exactly as used by spring's view abstraction.
	 */
	public void PreProcess(Map<String, Object> model);
	
	/**
	 * Processes the parameter map returned by the form post.
	 * @param inmap map returned by form post, all ready has any files passed extracted into a string
	 * @return the processsed map.
	 */
	public Map PostProcess(Map inmap);
	
}
