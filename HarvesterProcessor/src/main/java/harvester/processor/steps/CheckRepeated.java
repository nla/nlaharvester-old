package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** fails records that contain the specified repeated fields */
public class CheckRepeated extends GenericStep {
    private LinkedList<HashMap<String, String>> check_fields;

    
    public String getName() {
        return "Check Fields are Not Repeated";
    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);
        check_fields = (LinkedList<HashMap<String, String>>) props.get("Check Fields");
        if(check_fields == null)
            logger.info("no fields will be checked for you entered none.");
        
    }

    public Document processRecord(Document record, int position) throws Exception {
for(HashMap<String, String> map : check_fields) {
            int count = 0;
            String fieldXpath = map.get("Field Name");
            String regexp = map.get("Match Value");

            if (regexp == null || "".equals(regexp)) {
                regexp = ".*";
            }

            Pattern pattern;
            pattern = Pattern.compile(regexp, Pattern.DOTALL);

            List nodes = null;
            nodes = ((Document)record).selectNodes(fieldXpath);

            for ( Iterator iter = nodes.iterator(); iter.hasNext(); ) {
                Element element = (Element) iter.next();
                Matcher matcher = pattern.matcher(element.getText());
                if (matcher.matches()) {
                    count+=1;
                    if (count > 1) {
                        logger.logfailedrecord(StepLogger.RECORD_ERROR,"Record had repeated field", record, getPosition(), getName(), getStepId(), 
														position);
                        return null;
                    }
                }
            }
        }
        return record;
    }
}



