package harvester.processor.steps;

import harvester.data.Profile;
import harvester.processor.main.Records;
import harvester.processor.util.StepLogger;

import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.dom4j.Document;

public class ExampleLoader implements StagePluginInterface {
	
	private int position;
	private StepLogger logger;
	
	private Integer stepid;
	private String harvest_type; 
	
	public void Dispose() {
		//do any releasing of resources or cleaningup. will be called at the end of the harvest
	}

	public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
		//DO any initialization here. configuration from the properties file can be pulled out of the props object.
		stepid = (Integer)props.get("stepid");
		harvest_type =(String) props.get("type");
		this.logger = logger;
	}

	public Records Process(Records records) throws Exception {
		// Do something with the records here. One batch of records is passed to this at a time.
		
		//In all most all cases, you don't want this method to do anything for a test harvest
		if(String.valueOf(Profile.TEST_PROFILE).equals(harvest_type)) {
			logger.locallog("Test harvest, will not load into PEPO.", getName());
			return records;
		}
				
		logger.locallog("processing " + records.getRecords().size() + " records...", getName());
		
		//Loop over records not marked for deletion
		for(Iterator<Object> itor = records.getRecords().iterator(); itor.hasNext();) {
			Document record = (Document)itor.next();
			
			//Documents are standard dom4j documents.
			String xml = record.asXML();
		
		}
		
		
		//Loop over records marked for deletion
		for(Iterator<Object> itor = records.getDeletedRecords().iterator(); itor.hasNext();) {
			Document record = (Document)itor.next();
			
		}
		
		//Logger object can log to the machines log as well as the log page for the harvest in the UI
		logger.info(getName() + " processed batch");
		
		return records;
	}

	//used for logging
	public String getName() {
		return "Example Loader";
	}

	
	//The position is all ways set by the setter below
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

}
