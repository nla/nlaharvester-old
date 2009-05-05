package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.*;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Adds a field to the xml record */
public class AddField2 implements StagePluginInterface {
    private StepLogger logger;
    protected HashMap<String, Object> props;
    private Integer stepid;

    Pattern xpathPattern;
    Pattern attributePattern;

    public String getName() {
        return "Add Field";
    }

    public void Dispose() {}

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {

        logger.locallog("initializing " + getName(), getName());

        //set up xpath expressions
        xpathPattern = Pattern.compile("(.*)/(.*?)\\s+(.*)");
        attributePattern = Pattern.compile("\\s*(.+?)\\s*=\\s*'(.+?)'");

        this.props = props;
        this.logger = logger;
        stepid = (Integer)props.get("stepid");

    }


//  This should do the following:
//
//	multiple occurrences of match fields
//	=============================

//	1. If adding a new field when the match field is "not present or empty", We would only want to add the new field if *all* occurrences of the match field are empty.
//	e.g.
//	If we want to add a field if the subject is not present or empty, then we don't want to add it to the following record:
//	<subject>12345</subject>
//	<subject></subject>
//
//	2. If adding a new field when the match field has a particular value, We would always want to add the new field if *any *occurrence of the match field has the desired value.
//	e.g.
//	If we want to add a field if we find a subject matching the value 1234, then we do want to add the new field to the following record:
//	<subject>1234</subject>
//	<subject>5678</subject>
//
//	3. If more than one occurrence of the match field is found, only add the new field once.
//


    public Document ProcessRecord(Document record) throws Exception {
        int count = 0;
        String val = props.get("New Field Value").toString();
        String fieldXpath = props.get("New Field Name").toString();
        String match_field_xpath = "INVALID_VALUE";
        String value_matches = "";

        try {
            value_matches = props.get("Value Matches").toString();
        } catch (Exception e) {}

        try {
            match_field_xpath = props.get("Match Field Name").toString();
        } catch (Exception e) {}


        String add = props.get("Add").toString();

        XPathParser parser = new XPathParser(fieldXpath.trim());

        List nodes = null;
        Node parent = null;
        try {
            parent = record.selectSingleNode(parser.getPrefix());
            //logger.locallog(parser.getPrefix(), getName());
            nodes = record.selectNodes(match_field_xpath);
        } catch (Exception e) {
            logger.log(StepLogger.STEP_ERROR, "Failed at step " + getName() + " position " + getPosition() + ", XPath Expression was invalid <br />XPath: "
            		+ fieldXpath + "<br /> Cause: " + e.getMessage(), 
            		"XPath Expression was invalid", stepid, null);
            throw new Exception();
        }


        if (add.equals("1")) {					/* field not present */
            if (nodes.size() == 0) {
                return record;
            }
        } else if (add.equals("2")) {		/* field is present */
            if (nodes.size() > 0) {
                return record;
            }
        } else if (add.equals("3")) {		/* field not present, or blank */
for (Object node : nodes) {
                if (!((Node)node).getText().matches("^\\s*$")) {
                    return record;
                }
            }
        } else if (add.equals("4")) {		/* exists a field that matches some value */
            boolean valid = false;
for (Object node : nodes) {
                try {
                    if (((Node)node).getText().matches(value_matches)) {
                        valid = true;
                    }
                } catch (Exception e) {
                    logger.log(StepLogger.STEP_ERROR, "Failed at step " + getName() + " position " + getPosition() 
                    		+ ", Regular Expression was invalid <br />Regular Expression: " + value_matches + "<br /> Cause: " + e.getMessage()
                    		, "Regular Expression was invalid", stepid,  null);
                    throw new Exception();
                }

            }
            if (!valid) {
                return record;
            }
        }
        if (parser.isAttribute()) {
            if (parent instanceof Element) {  /* attributes can only be added to elements */
                ((Element)parent).addAttribute(parser.getAttribute(), val);
            } else {
                logger.log(StepLogger.STEP_ERROR, "Failed at step " + getName() + " position " + getPosition() 
                		+ ", XPath Expression was invalid <br />XPath: " + fieldXpath + "<br /> Cause: Attributes can only be added to element nodes."
                		,  "XPath Expression was invalid", stepid, null);
                throw new Exception();
            }


        } else {
            if (parent instanceof Branch) { /* elements can only be added to branches */
                Element new_element = ((Branch)parent).addElement(parser.getElement());
                new_element.addText(val);
                for (XPathParser.Attribute a : parser.getAttributes()) {
                    new_element.addAttribute(a.getName(), a.getValue());
                }
            } else {
                logger.log(StepLogger.STEP_ERROR, "Failed at step " + getName() + " position " + getPosition() 
                		+ ", XPath Expression was invalid <br />XPath: " + fieldXpath + "<br /> Cause: Elements can only be added to branche nodes."
                		,  "XPath Expression was invalid", stepid, null);
                throw new Exception();
            }
        }
        return record;
    }

    public Records Process(Records records) throws Exception {

        logger.locallog("processing", getName());

        Records processed_records = new Records(records);

        File f = null;
for(Object record : records.getRecords()) {
            processed_records.addRecord(ProcessRecord((Document)record));
        }
        return processed_records;
    }

    private int position;

    public int getPosition() {
        // TODO Auto-generated method stub
        return position;
    }

    public void setPosition(int position) {
        // TODO Auto-generated method stub
        this.position = position;
    }
}



