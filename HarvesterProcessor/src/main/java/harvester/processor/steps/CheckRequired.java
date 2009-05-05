package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** fails any records that do not have the required fields */
public class CheckRequired extends GenericStep {
    private LinkedList<HashMap<String, String>> check_fields;

    private Integer stepid;
    
    public String getName() {
        return "Check for Required Fields";
    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);

        check_fields = (LinkedList<HashMap<String, String>>) props.get("Check Fields");
        if(check_fields == null)
            logger.info("no fields will be checked for you entered none.");
        
        stepid = (Integer)props.get("stepid");
    }

    public Document processRecord(Document record, int position) throws Exception {
for(HashMap<String, String> map : check_fields) {

            String fieldXpath = map.get("Field Name");
            String regexp = map.get("Required Value");
            String matchAll = map.get("Match all occurrences");

            if (regexp == null || regexp.equals("")) {
                regexp = ".*";
            }
            Pattern pattern;
            pattern = Pattern.compile(regexp, Pattern.DOTALL);

            List nodes = record.selectNodes(fieldXpath);

            if (nodes.size() == 0) {
                logger.logfailedrecord(StepLogger.RECORD_ERROR, "Failed required field check on field " + fieldXpath, record, getPosition(), getName(), stepid, position);
                return null;
            }

            int matchcount = 0;
            for ( Iterator iter = nodes.iterator(); iter.hasNext(); ) {
                Element element = (Element) iter.next();
                Matcher matcher = pattern.matcher(element.getText());
                if ( matcher.matches() )
                    matchcount++;
            }

            if( matchcount != nodes.size() && "on".equals(matchAll) ) {
                logger.logfailedrecord(StepLogger.RECORD_ERROR, "Failed required field check on field " + fieldXpath, record, getPosition(), getName(), stepid, position);
                return null;
            }

            if(matchcount == 0) {
                logger.logfailedrecord(StepLogger.RECORD_ERROR, "Failed required field check on field " + fieldXpath, record, getPosition(), getName(), stepid, position);
                return null;
            }
        }
        return record;
    }


}



