package harvester.processor.steps;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import java.util.* ;
import javax.servlet.ServletContext;

//
/**
 * This is the interface a step must implement to do the actual processing for the step.
 * There are a few things that needed to be known by plugin implementors:
 * <ul>
 *  <li>All logging should be done through the passed in StepLogger object. Logging error messages should
 *  be done using logger.log(level, msg, data), info messages should be though logger.info(msg). Both of these
 *  log both the the user viewable logs and the local logs. Messages not important enough for the user to see
 *  should be logged using logger.locallog. Error messages that are sent from within a catch block, that do not 
 *  need to be seen in the GUI but should show the stack trace can be sent with logger.error. See the 
 *  logger.logfailedrecord method for a helper method for logging failed records that are in dom4j format. </li>
 *  <li>The passed in servlet context object can be used for accessing the local directory structure. This can be
 *  used for accessing folders that contain stylesheets or schemas. an example of this is <br /> 
 *  this.transformer = factory.newTransformer(new StreamSource(servletContext.getRealPath(folder + ss))); </li> 
 *  <li> The passed properties object contains both properties specified for this step in the local configuration
 *  and the properties that were entered in the GUI. the way to access them is as follows. Note that there are 
 *  two types of properties, singular properties and nested properties. nested properties are the ones that 
 *  appear in the view with the "Add Another" and "Remove" buttons so that you can have multiples.
 *  	<ul>
 *  		<li>For a single property that would appear in the gui as a drop down, textbox, checkbox or a set of
 *  			tickboxes it is retrieved like "String ss = props.get("stylesheet").toString();". stylesheet is
 *  			the name exactly as it is next to the field in the gui. For radio buttons the numbers in the html
 *  			are the values that the field will take in the code. For checkboxes, if they are checked the
 *  			value will be "on" if it is ticked, and either null or empty otherwise. use "on".equals(field)
 *  			to match for this case. </li>
 *  		<li>A nested property contains a set of fields that are grouped together, and the user can enter
 *  			any number of groups. This is in the props map under the name that appears in the far left in
 *  			the gui. The datatype in the map is LinkedList&#060;HashMap&#060;String, String&#062;&#062;. Each row the user
 *  			entered in the gui is a hashmap in the list. The hashmap behave just like the main properties map
 *  			except they only contain the fields that are nested and the corresponding data entered into that
 *  			row.</li>
 *  		<li>Properties in the local config file can be accessed by name. e.g. If your step is id 4, then you
 *  			might have the property "4.stylesheet=WEB-INF/stylesheets" in the config file. Then this
 *  			Is accessed using "String ss = props.get("stylesheet").toString();" Properties that are global
 *  			in the configuration file are also accessable. e.g. "all.fish=fish" is a global property passed
 *  			to all steps.</li>
 *  		<li>Many of the harvest specific data is also passed in this map. The following in fact:
 *  			harvestid, harvestfrom, harvestuntil, type, delete, until50, singlerecord(if applicable), 
 *  			contributorid, stage</li>
 *  	</ul>
 *  </li>
 *  <li>
 *  	There is a very specific protocol for exceptions that should be thrown from steps. <br />
 *  	If this is a harvest step, there are three possiblities:
 *  	<ul>
 *  		<li> If you throw an exception that subclasses CustomException, then the status message shown will be the
 *  			 one returned by the getStatusMessage method of the thrown exception class</li>
 *  		<li> Throwing an exception of type UnableToConnectException will result in the harvest being rescheduled
 *  			 some time in the future with a harvest status FAILED or WARNING, and any emails will be sent as setup.
 *  			 A message such as "Unable to connect, retrying at: " + timetodo.toString() will be shown in the user
 *  			 logs </li>
 *  		<li> An InterruptedException should not be directly thrown in a step, but can be caused by the server 
 *  			 shutting down. This will result in the ending imediately with status succesful in those cases. </li>
 *  		<li> Any other exception will result in the harvest ending, the status being set to "FAILED". If the 
 *  			 exception contains no msg text then the user will see in the gui logs "FAILED", otherwise they will
 *  			 see "FAILED, Java Error:" + (text of error).</li>
 *  	</ul>
 *  </li>
 *  </ul>
 *  
 */
public interface StagePluginInterface {

    public String getName();

    /**Called after construction but before any records are processed. **/
    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext ) throws Exception;

    /**Called once to process records */
    public Records Process(Records records) throws Exception;

    /**Called after initialise, but possibily with a process step going on. For cleanup */
    public void Dispose();

    public int getPosition();
    /** set so that the step knows its position in the pipeline, useful for logging */
    public void setPosition(int position);


}
