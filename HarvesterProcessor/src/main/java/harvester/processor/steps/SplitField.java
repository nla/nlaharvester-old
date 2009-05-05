package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import converter.*;
import converter.operations.*;
import converter.preconditions.*;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;

/** Split a field from a xml record */
public class SplitField extends GenericStep {
    private StepLogger logger;
    protected HashMap<String, Object> props;

    private LinkedList<HashMap<String, String>> check_fields;

    public String getName() {
        return "Split Field";
    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);
        check_fields = (LinkedList<HashMap<String, String>>) props.get("Fields");
        if(check_fields == null) {
            logger.info("no fields will be checked for you entered none.");
        }
    }

    public Document processRecord(Document record, int position) throws Exception {
for(HashMap<String, String> map : check_fields) {
            int i = 0;
            String fieldXpath = map.get("Field Name");
            String regexp = map.get("Delimiter");

            Filter.apply(record, new OpSplit(regexp), fieldXpath, null);
        }
        return record;
    }
}



