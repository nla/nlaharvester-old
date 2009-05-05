package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import converter.*;
import converter.operations.*;
import converter.preconditions.*;


/** Delete a field from a xml record */
public class DeleteField extends GenericStep {
    private LinkedList<HashMap<String, String>> check_fields;

    public String getName() {
        return "Delete Field";
    }

    @SuppressWarnings("unchecked")
	public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);
        check_fields = (LinkedList<HashMap<String, String>>) props.get("Fields");
    }

    public Document processRecord(Document record, int position) throws Exception {
    	for(HashMap<String, String> map : check_fields) {
            String fieldXpath = map.get("Field Name");
            String regexp = map.get("Field Value");
            Filter.apply(record, new OpDelete(), fieldXpath, new TextMatches(regexp));
        }
        return record;
    }

}



