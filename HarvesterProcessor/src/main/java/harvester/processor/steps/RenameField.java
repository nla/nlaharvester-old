package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import converter.*;
import converter.operations.*;
import converter.preconditions.*;

/** Rename a field */
public class RenameField extends GenericStep {

    private LinkedList<HashMap<String, String>> check_fields;

    public String getName() {
        return "RenameField";
    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);
        check_fields = (LinkedList<HashMap<String, String>>) props.get("Fields");
    }

    public Document processRecord(Document record, int position) throws Exception {
for(HashMap<String, String> map : check_fields) {
            int count = 0;
            String fieldXpath = map.get("Field Name");
            String new_name = map.get("Field Value");
            Filter.apply(record, new OpRename(new_name), fieldXpath, null);
        }
        return record;
    }
}


