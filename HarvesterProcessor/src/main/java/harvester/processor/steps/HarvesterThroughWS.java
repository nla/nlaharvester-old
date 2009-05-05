package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.*;
import harvester.processor.exceptions.*;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Rename a field */
public class HarvesterThroughWS extends GenericStep {
    private OaiClient client;
    boolean get_50;
    private String forced_encoding;
    private Integer stepid;
    private int total_normal_records_so_far = 0;

    public String getName() {
        return "OaiStep";
    }
    //used for injecting mock objects
    public OaiClient getOaiClient() {
        return client;
    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);

        //String url = props.get("url").toString();
        String base_url = props.get("Base URL").toString();
        String from = (props.get("harvestfrom") == null ? null : props.get("harvestfrom").toString());
        String until = (props.get("harvestuntil") == null ? null : props.get("harvestuntil").toString());
        String set = (props.get("Set") == null ? null : props.get("Set").toString());
        String metadata_prefix = props.get("Metadata Prefix").toString();
        String record_oai_id = (props.get("singlerecord") == null ? null : props.get("singlerecord").toString());

        forced_encoding = (String) props.get("Encoding");
        //boolean get_record = "true".equals(props.get("stopatfirst"));
        get_50 = "true".equals(props.get("until50"));

        stepid = (Integer)props.get("stepid");
        
        //Its vitial that the above strings get logged using a log properties call, otherwise they
        //will not show up under the Harvest Details heading in the logs.
        logger.logprop("Base URL", base_url, stepid);
        logger.logprop("From", from == null ? "-" : from, stepid);
        logger.logprop("Until", until == null ? "-" : until, stepid);
        logger.logprop("Metadata Prefix", metadata_prefix == null ? "-" : metadata_prefix, stepid);
        logger.logprop("Set", set == null ? "-" : set, stepid);
        if(get_50 == true) logger.logprop("Stop at 50 records", "True", stepid);
        if(record_oai_id != null) logger.logprop("identifier", record_oai_id, stepid);


        if (record_oai_id != null) {	//is single record harvest?
            client = new OaiClient(base_url, record_oai_id, metadata_prefix, logger, forced_encoding, stepid);
        } else {
            client = new OaiClient(base_url, set, metadata_prefix, from, until, logger, forced_encoding, stepid);
        }
    }

    public Document processRecord(Document record, int position) throws Exception {
        return null;
    }

    public String getNamespace(String response) throws Exception {
        String namespaces = "";

        Pattern pattern = Pattern.compile("<\\s*OAI.PMH(.*?)>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String header = matcher.group(1);
            logger.locallog("found the following header: " + header, getName());
            pattern = Pattern.compile("xmlns:([^=]*)=\"([^\"]*)\"");
            matcher = pattern.matcher(header);
            while (matcher.find()) {
                namespaces += "xmlns:" + matcher.group(1) + "=\"" + matcher.group(2) + "\" ";
            }
        } else {
            throw new Exception("Document head is not in OAI format.");
        }
		logger.locallog("namespaces = " + namespaces, getName());
        return namespaces;
    }

    private void addOAIIdentifierComment(Document record) {
        Node n = record.selectSingleNode("/record/header/identifier");
        if(n != null) {
            record.addComment("identifier=" + n.getText());
        } else {
            logger.locallog("no identifer for record!", getName());
        }
    }


    public void parseResponse(Records records, String response) throws Exception {
        String namespaces = getNamespace(response);

        Matcher matcher;

        Pattern record_tag = Pattern.compile("<\\s*record(\\s*|\\s.*?)>|<\\s*/\\s*record\\s*>");
        int count = 0;
        int parse_error_count = 0;


        String xml = "";
        while (true) {
            int level = 0;
            int start = -1;
            /* Now splits records by finding and counting opening and closing record tags. Should be more robust than the previous regex */
            matcher = record_tag.matcher(response);
            while (matcher.find()) {
                if (matcher.group(0).matches("<\\s*/\\s*record\\s*>")) {
                    level-=1;
                } else {
                    level+=1;
                }
                if (start == -1) {
                    start = matcher.end();
                }
                if (level == 0) {

                    xml = "<record xmlns:oai=\"http://www.openarchives.org/OAI/2.0/\" " + namespaces +">" + response.substring(start, matcher.start()) + "</record>";
                    response = response.substring(matcher.end());
                    break;
                }
            }
            if (level != 0) {
                logger.log(StepLogger.STEP_ERROR, "OAI Error<br/>Malformed XML could not be parsed", "Malformed XML", stepid, null);
                throw new HarvestException();
            }

            if (start == -1) {
                break;
            }

            if (get_50 && total_normal_records_so_far >= 50) {
                records.setContinue_harvesting(false);
                logger.locallog("Harvested 50 records, breaking from record loop", getName());
                break;
            }
            Document record;
            try {
                record = DocumentHelper.parseText(xml);
                addOAIIdentifierComment(record);
                if (record.selectSingleNode("/record/header[@status='deleted']") != null) {
                    records.addDeletedRecord(record);
                } else {
                    total_normal_records_so_far++;                	
                    records.addRecord(record);
                }
            } catch (Exception e) {
                logger.logfailedrecord("Record " + count + " rejected" +
                                       "<br/>Processing Step: " + getName() +
                                       "<br/>Reason: Record malformed<br/>" + e.getMessage(), "Record malformed", stepid, xml);
                parse_error_count++;
            }
            count++;
        }
        logger.locallog(count + " records in batch", getName());
        logger.locallog(parse_error_count + " records in batch had parse errors", getName());
        logger.locallog(records.getCurrentrecords() + parse_error_count + " total records", getName());

        if (client.getCompleteListSize() != null) {
            records.setRecordsinsource(client.getCompleteListSize());
            logger.locallog(client.getCompleteListSize() + " records in repository", getName());
        }

        if (client.getCursor() != null) {
            logger.locallog("cursor at " + client.getCursor(), getName());
        }
        records.setTotalRecords(records.getCurrentrecords() + parse_error_count);

    }


    public Records Process(Records records) throws Exception {

        String response = null;

        try {
            logger.locallog("Requesting next batch", getName());
            response = client.getNext();
            logger.locallog("Request made was", client.getRequest());

        } catch (OaiException e) {
            logger.locallog("Exception Raised by client.getNext()", getName());
            /*      * badArgument - The request includes illegal arguments or is missing required arguments.
            	    * badResumptionToken - The value of the resumptionToken argument is invalid or expired.
            	    * cannotDisseminateFormat - The value of the metadataPrefix argument is not supported by the repository.
            	    * noRecordsMatch - The combination of the values of the from, until, set and metadataPrefix arguments results in an empty list.
            	    * idDoesNotExist - The value of the identifier argument is unknown or illegal in this repository.
            	    * noSetHierarchy - The repository does not support sets.
             		*/

            if("noRecordsMatch".equals(e.getCode().trim())) {
                logger.info("Received a noRecordsMatch response");
                records.setContinue_harvesting(false);
                response = null;
            } else {
                logger.log(StepLogger.STEP_ERROR, "OAI Error<br/>Error Code: " + e.getCode() +
                           "<br/>Response: " + e.getMessage(), e.getCode(), stepid, null);
                throw new HarvestException();
            }
        } catch ( AttemptRetryException e) {
            logger.error("got attempt retry exception", e);
            throw new UnableToConnectException();
        } catch ( ConnectionException e) {
            logger.error("connection exception", e);
            String code = e.getResponseCode() + " : " + client.getStatusStringFromCode(e.getResponseCode());
            logger.log(StepLogger.STEP_ERROR, "Connection Error <br/>Response Code: " + code 
                       + (e.getMessage() == null ? "" : "<br/>Description: " + e.getMessage()), code, stepid, null);
            throw new HarvestException();
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException Raised by client.getNext()", e);
            throw new HarvestException();
        } catch (Exception e) {
            logger.error("Exception Raised by client.getNext()", e);
            throw new HarvestException();
        }

        logger.locallog("Request was " + client.getRequest(), getName());
        if (!client.hasNext()) {
            records.setContinue_harvesting(false);
        }
        if (response == null) {
            return records;
        }

        logger.locallog("response was " + response.length() + " tokens long", getName());
        try {
        	parseResponse(records, response);
        } catch (Exception e){
        	logger.error("error parsing reponse", e);
            logger.log(StepLogger.STEP_ERROR, "Incorrect Response Received.<br />Error: " + e.getMessage(), "Harvest Error", stepid, response);
            throw new HarvestException();
        }

        return records;
    }
}


