package harvester.processor.steps;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;
import javax.servlet.ServletContext;
import java.util.*;


/** Delete a field from a XML record */
public class ClassStep implements StagePluginInterface {
    private int position;
    private StagePluginInterface converter;

    public String getName() {
        return "Class Step";
    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        converter = (StagePluginInterface)Class.forName((String)props.get("converter")).newInstance();
		logger.locallog("Loaded " + (String)props.get("converter"), getName());
		converter.setPosition(position);
		
		converter.Initialise(props, logger, servletContext);
    }

    public Records Process(Records records) throws Exception {
		return converter.Process(records);
	}

	public void Dispose() {}
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }

}



