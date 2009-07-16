package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;

public abstract class GenericStep implements StagePluginInterface {
	protected StepLogger logger;
	protected HashMap<String, Object> props;
	protected int position;
	protected Integer stepid;
	//protected boolean execute = true;

	public Integer getStepId() {
		return stepid;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public abstract String getName();

	public void Dispose() {}

	public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
		logger.locallog("init called, position=" + position, getName());
		this.props = props;
		this.logger = logger;
		this.stepid = (Integer)props.get("stepid");
		logger.locallog("initialized " + getName() + " stepid " + stepid, getName());
	}

	public abstract Document processRecord(Document record, int record_position) throws Exception;

	public Records Process(Records records) throws Exception {
		
		logger.locallog("processing", getName());

		int count = 0;

		Records processed_records = new Records(records);

		for(Iterator<Object> itor = records.getRecords().iterator(); itor.hasNext();) {
			Document record = (Document)itor.next();
			List<Comment> comments = record.selectNodes("comment()");

			try {
				record = processRecord(record, count);

			} catch (Exception e) {
				logger.log(StepLogger.STEP_ERROR, "Fatal Step Error<br />Step: " + getPosition() + "." + getName() 
						+ "<br />Msg: " + e.getMessage(), "Fatal Step Error", stepid, null);
				logger.error("processing step error: " + e.getMessage(), e);
				throw new Exception();
			}

			if (record != null) {
				int remaining_comment_nodes = record.selectNodes("comment()").size();
				// if we lose the identifier in the comments, put it back
				if(remaining_comment_nodes == 0) { 
					for(Comment n : comments)
						record.add(n);
				}
				processed_records.addRecord(record);
			}
			
			count++;
		}
		return processed_records;
	}	
	
}



