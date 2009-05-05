package harvester.processor.steps;

import harvester.processor.main.*;
import harvester.processor.util.*;
import harvester.processor.exceptions.*;
import harvester.data.*;
import harvester.processor.data.dao.*;
import harvester.processor.data.dao.interfaces.*;

import org.dom4j.*;
import org.dom4j.io.*;
import java.net.*;

import javax.servlet.ServletContext;
import java.util.regex.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;

public class ArrowLoader implements StagePluginInterface {
    private static final int RECORD_NOT_PRESENT = 0;
    private static final int RECORD_PRESENT = 1;
    private static final int RECORD_DIFFERENT_CONTRIBUTOR = 2;
    
    private Integer stepid;

    private StepLogger logger;
    protected HashMap<String, Object> props;
    String solr_url;
    String arrow_url;
    private DAOFactory daofactory;
    private HarvestdataDAO harvestdatadao;

    //cumulative counts
    private int new_records_cumu = 0;
    private int updated_records_cumu = 0;
    private int updated_changed_records_cumu = 0;
    private int deleted_records_cumu = 0;
    private int deleted_not_present_cumu = 0;

	private DateFormat userdateformater;

    private int position;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return "ArrowLoader";
    }

    public void Dispose() {
        String loadfortest = (String) props.get("loadfortest");
        String type = (String) props.get("type");

        if(String.valueOf(Profile.PRODUCTION_PROFILE).equals(type) || "true".equals(loadfortest) ) {
//			logger.locallog("optimizing", getName());
//			postString("<optimize />");

            if (updated_changed_records_cumu > 0) {
                logger.info("Records Count: Inserted: " + new_records_cumu + ", Updated: " + updated_records_cumu 
                		+ " (including " + updated_changed_records_cumu + " new records that already existed for another contributor), Deleted: "
                		+ deleted_records_cumu + ", Deleted (that were not found): " + deleted_not_present_cumu);
            } else {
                logger.info("Records Count: Inserted: " + new_records_cumu + ", Updated: " + updated_records_cumu 
                		+ ", Deleted: " + deleted_records_cumu + ", Deleted (that were not found): " + deleted_not_present_cumu);
            }

        }

    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger,
                           ServletContext servletContext) throws Exception {

        this.props = props;
        this.logger = logger;

		userdateformater = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		userdateformater.setTimeZone(TimeZone.getDefault());
        
        this.solr_url = (String)props.get("solr_url");
        this.arrow_url = (String)props.get("arrow_url");
        this.stepid = (Integer)props.get("stepid");
        
        daofactory = DAOFactory.getDAOFactory();
        harvestdatadao = daofactory.getHarvestdataDAO();

        if (this.props.get("delete") == null) {
            this.props.put("delete", "none");
        }

        logger.locallog("initializing " + getName(), getName());
    }

    public Date parseDate(String str) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return (Date)formatter.parse(str);
        } catch (Exception e) {}

        try {
            formatter = new SimpleDateFormat("yyyy-MM-ddThh:mm:ssZ");
            return (Date)formatter.parse(str);
        } catch (Exception e) {
            logger.log(StepLogger.STEP_ERROR,"couldn't parse oai datestamp", "Invalid datestamp", stepid, null);
        }
        return new Date();
    }

    public void setDatestamp(Document record) {
        Element element = (Element)record.selectSingleNode("doc/field[@name='oaidatestamp']");
        String text = element.getText();

        element.setText(Long.toString(parseDate(text).getTime()));
    }

    public void callService(String str) {
        HttpURLConnection conn = null;
        try {
            logger.locallog("url :" + str.toString(), getName());

            logger.locallog("connecting", getName());
            URL requesturl = new URL(str);
            conn = (HttpURLConnection) requesturl.openConnection();
            conn.setReadTimeout(600000);
            conn.setConnectTimeout(600000);

            conn.connect();

        } catch (Exception e) {
            logger.locallog("error, msg=" + e.getMessage(), getName());
            try {
                switch (conn.getResponseCode()) {
                case 200:
                case 301:
                case 302:
                    break;
                default:
                    logger.log(StepLogger.STEP_ERROR, "Failed to connect to webservice: \"" + str + "\" " + conn.getResponseMessage()
                    		, "Failed to connect to webservice" , stepid, null);
                    break;
                }
            } catch (Exception f) {
                // this is silly.
                logger.log(StepLogger.STEP_ERROR, "Failed to connect to webservice: \"" + str + "\" "
                		, "Failed to connect to webservice" , stepid, null);
            }
        } finally {
            try {
                if(conn != null)
                    conn.disconnect();
            } catch (Exception e) {
                logger.log("error disconnecting: " + e.getMessage());
            }
        }
    }
    public void postString(String str) throws Exception {
        int attempts = 0;

        while(attempts < 3) {

            if (attempts > 0) {
                logger.log(StepLogger.STEP_ERROR, "Problem sending data to Arrow Solr. Retrying update command in 5 minutes [Local Time: " 
                		+ userdateformater.format(new Date()) + "]", "Problem sending data to Arrow Solr" , stepid, null);
                Thread.sleep(300*1000);	// 5 minutes
            }
            try {
                SimplePostTool tool	= new SimplePostTool(new URL(solr_url + "/update"));
                //StringReader reader = new StringReader(str);
                InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(str.getBytes("UTF-8")), "UTF-8");
                StringWriter writer = new StringWriter();
                tool.postData(reader, writer);

                return;	//success!!
            } catch (Exception e) {
                attempts++;
                logger.locallog("error with url " + solr_url + "/update", getName());
                logger.locallog("error message is" + e.getMessage(), getName());
                for(StackTraceElement el : e.getStackTrace())
                    logger.locallog(el.toString(), getName());
            }
        }
        //////FAILURE!!!!!
        logger.log(StepLogger.STEP_ERROR, "All retries failed!, Stopping the harvest.", "Arrow connection error", stepid, null);
        throw new StorageException();
    }

    public void delete_document(String id) {
    }

    public Document getSolrDocument(String query) throws Exception {
        int attempts = 0;
        logger.locallog("Getting solr document with query : " + query, getName());
        while (attempts < 3) {
            if (attempts > 0) {
                logger.log(StepLogger.STEP_ERROR, "Problem getting document from Arrow Solr. Retrying select command in 5 minutes [Local Time: " 
                		+ userdateformater.format(new Date()) + "]", "Problem getting document from Arrow Solr", stepid, null);
                Thread.sleep(300*1000); //5 minutes
            }
            HttpURLConnection c = null;
            URL url = null;
            try {
                //logger.locallog("building url", getName());
                url = new URL(solr_url + "/select?" + URLEncoder.encode("fl=inst_type,contributed_date,la_created_date,la_updated_date,primary_inst,identifier,oaiid,state,inst", "UTF-8") + "&limit=1&q=" + URLEncoder.encode(query, "UTF-8"));
//				url = new URL(solr_url + "/select?&limit=1&q=" + URLEncoder.encode(query, "UTF-8"));
                c = (HttpURLConnection)url.openConnection();
                BufferedInputStream buffer = new BufferedInputStream(c.getInputStream());

                SAXReader xmlReader = new SAXReader();
                xmlReader.setValidation(false);
                return xmlReader.read(buffer);
            } catch (Exception e) {
                try {
                    attempts++;
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw, true);
                    e.printStackTrace(pw);
                    pw.flush();
                    sw.flush();
                    logger.locallog("error message = " + e.getMessage() + " stacktrace : " + sw.toString(), getName());
                    if (c.getResponseCode() == 400) {
                        logger.log(StepLogger.STEP_ERROR, "Bad Request, url = " + url, "Bad Request", stepid, null);
                        return null;
                    }
                } catch(Exception e2) {
                    logger.locallog("error message = " + e2.getMessage(), getName());
                    logger.log(StepLogger.STEP_ERROR, "Unable to discern response code", "Unable to discern response code" , stepid, null);
                }
            }
        }
        logger.log(StepLogger.STEP_ERROR, "Arrow Solr is currently down. Unable to upload records into Arrow. Stopping the harvest."
        		, "Arrow Solr is down" , stepid, null);
        throw new StorageException();
    }

    public String getSolrDocumentElement(Document solr_doc, String name) {
        return solr_doc.selectSingleNode("response/result/doc/str[@name='" + name + "']").getText();
    }

    public String getSolrDocumentID(Document solr_doc) {
        return solr_doc.selectSingleNode("response/result/doc/str[@name='id']").getText();
    }

    public boolean updateDoc(Document solr_doc, Document record, String contributor_name) {
        List nodes;
        nodes = solr_doc.selectNodes("response/result/doc/arr[@name='inst']/str");
        boolean new_contributor = true;
        int position = -1;
        int i = 0;
for (Object node : nodes) {
            String inst = ((Element)node).getText();
            if (!inst.equals(contributor_name)) {
                record.getRootElement().addElement("field").addAttribute("name", "inst").addText(inst);
            } else {
                position = i;
                new_contributor = false;
            }
            i++;
        }
        nodes = solr_doc.selectNodes("response/result/doc/arr[@name='state']/str");
        i = 0;
for (Object node : nodes) {
            String state = ((Element)node).getText();
            if (i!=position)
                record.getRootElement().addElement("field").addAttribute("name", "state").addText(state);
            i++;
        }
        i = 0;
        nodes = solr_doc.selectNodes("response/result/doc/arr[@name='oaiid']/str");
for (Object node : nodes) {
            String oaiid = ((Element)node).getText();
            if (i!=position)
                record.getRootElement().addElement("field").addAttribute("name", "oaiid").addText(oaiid);
            i++;
        }
        i = 0;
        nodes = solr_doc.selectNodes("response/result/doc/arr[@name='inst_type']/str");
for (Object node : nodes) {
            String type = ((Element)node).getText();
            if (i!=position)
                record.getRootElement().addElement("field").addAttribute("name", "inst_type").addText(type);
            i++;
        }

        nodes = solr_doc.selectNodes("response/result/doc/str[@name='created_date_la']");
for (Object node : nodes) {
            String created_date_la = ((Element)node).getText();
            record.getRootElement().addElement("field").addAttribute("name", "created_date_la").addText(created_date_la);
            break;
        }
        return new_contributor;
    }

    public void setPrimaryInst(Document record) {
        String min = "";
        List nodes = record.selectNodes("doc/field[@name='inst']");
for (Object node : nodes) {
            if (min.equals("") || ((Element)node).getText().compareTo(min) < 0) {
                min = ((Element)node).getText();
            }
        }
        ((Document)record).getRootElement().addElement("field")
        .addAttribute("name", "primary_inst")
        .addText(min);
    }

    public int getSolrDocumentHits(Document solr_doc) {
        if (solr_doc == null) {
            return 0;
        }
        Element element = (Element)solr_doc.selectSingleNode("response/result");
        if (element != null) {
            return Integer.parseInt(element.attribute("numFound").getValue());
        }
        return 0;
    }


    public int checkRecord(Document record, int index) throws Exception {
        String identifier = "";
        String oai_identifier = "";
        String contributor_name = "";

        Date date = new Date();

        List nodes = record.selectNodes("doc/field[@name='identifier']");
for (Object node : nodes) {
            String val = ((Node)node).getText();
            if (val.startsWith("http")) {
                identifier = val;
            }
        }

        Node contributor_node = record.selectSingleNode("doc/field[@name='inst']");
        if (contributor_node != null) {
            contributor_name = contributor_node.getText();
        }

        Node oai_node = record.selectSingleNode("doc/field[@name='oaiid']");
        if (oai_node != null) {
            oai_identifier = oai_node.getText();
        }
        Document solr_doc = null;
        solr_doc = getSolrDocument("identifier:\"" + identifier + "\"");

        if (solr_doc != null && getSolrDocumentHits(solr_doc) == 1) {
            nodes = solr_doc.selectNodes("response/result/doc/arr[@name='identifier']/str");
for (Object node : nodes) {
                if (((Node)node).getText().equals(identifier)) {
                    boolean new_contributor = updateDoc(solr_doc, record, contributor_name);
                    record.getRootElement().addElement("field").addAttribute("name", "id").addText(getSolrDocumentID(solr_doc));
                    if (new_contributor) {
                        return RECORD_PRESENT | RECORD_DIFFERENT_CONTRIBUTOR;
                    }
                    return RECORD_PRESENT;
                }
            }
        }

        solr_doc = getSolrDocument("oaiid:\"" + oai_identifier + "\" AND inst:\"" + contributor_name + "\"");
        if (solr_doc != null && getSolrDocumentHits(solr_doc) >= 1) {
            record.getRootElement().addElement("field").addAttribute("name", "id").addText(getSolrDocumentID(solr_doc));
            boolean new_contributor = updateDoc(solr_doc, record, contributor_name);
            if (new_contributor) {
                return RECORD_PRESENT | RECORD_DIFFERENT_CONTRIBUTOR;
            }
            return RECORD_PRESENT;
        }
        record.getRootElement().addElement("field")
        .addAttribute("name", "id")
        .addText("oai:arrow.nla.gov.au:" + Long.toString(date.getTime()) + Integer.toString(index));
        return RECORD_NOT_PRESENT;
    }


    public Records Process(Records records)
    throws Exception {
        logger.locallog("processing", "ArrowLoader");
        Records processed_records = new Records(records);
        Node n;
        //if this is a test harvest, the configuration file will say weather we should load or not
        String loadfortest = (String) props.get("loadfortest");
        String type = (String) props.get("type");

        if(String.valueOf(Profile.TEST_PROFILE).equals(type) && !"true".equals(loadfortest) ) {
            logger.locallog("Test harvest, will not load into arrow.", getName());
            return records;
        }

        Document document = DocumentHelper.createDocument();
        Element add = document.addElement("add");

        Contributor cont = ((Contributor)props.get("contributor"));

        String contributed_date = "";
        String updated_date = "";
        String institution = cont.getName();

        Date date = new Date();
        Calendar calendar = Calendar.getInstance();

        contributed_date = Long.toString(date.getTime());

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        updated_date = format.format(date);

        int updated_records = 0;
        int updated_changed_records = 0;
        int new_records = 0;

        int i = 0;

        /* REMOVED FROM INTERFACE: Broken anyway, needs to be in dispose
        /*		if (((String)props.get("delete")).equals("all")) {
        	postString("<delete><query>inst:\"" + institution + "\"</query></delete>");	
        }*/

for(Object record : records.getRecords()) {
            boolean skip = false;
            i++;
            List nodes = ((Document)record).selectNodes("/doc/field[@name='identifier']");
            if (nodes.size() < 1) {
                logger.logfailedrecord(StepLogger.STEP_ERROR, "These records could not be stored as they are not in the expected format. Could not find identifier element"
                		, (Document)record, position, getName(), stepid, i);
                continue;
            }

            int min = 1000000;
            nodes = ((Document)record).selectNodes("/doc/field[@name='date']");

for (Object node : nodes) {
                if ( ((Element)node).getText() == null || ((Element)node).getText().equals("") || !((Element)node).getText().matches("^[0123456789]+$") ) {
                    skip = true;
                } else {
                    int val = Integer.parseInt(((Element)node).getText());
                    if (val < min) {
                        min = val;
                    }
                    ((Element)node).setText(Integer.toString(val));
                }
            }
            nodes = ((Document)record).selectNodes("/doc/field[@name='decade']");

for (Object node : nodes) {
                if ( ((Element)node).getText() == null || ((Element)node).getText().equals("") || !((Element)node).getText().matches("^[0123456789]+$") ) {
                    skip = true;
                } else {
                    int val = Integer.parseInt(((Element)node).getText());
                    ((Element)node).setText(Integer.toString(val));
                }
            }

            if (skip) {
                logger.logfailedrecord(StepLogger.RECORD_ERROR, "Had an illegal date or decade in record which would crash solr"
                		, (Document)record, position, getName(), stepid, i);
                continue;
            }

            processed_records.addRecord((Document)record);
            if (min < 3000) {
                ((Document)record).getRootElement().addElement("field")
                .addAttribute("name", "primary_date")
                .addText(Integer.toString(min));
            }

            setDatestamp((Document)record);
            ((Document)record).getRootElement().addElement("field")
            .addAttribute("name", "inst")
            .addText(institution);
            ((Document)record).getRootElement().addElement("field")
            .addAttribute("name","contributed_date")
            .addText(contributed_date);
            ((Document)record).getRootElement().addElement("field")
            .addAttribute("name","updated_date_la")
            .addText(updated_date);

            n = ((Document)record).selectSingleNode("/doc/field[@name='inst_type']");
            if (n == null) {
                ((Document)record).getRootElement().addElement("field")
                .addAttribute("name","inst_type")
                .addText("uni" + institution);
            } else {
                ((Element)n).setText(((Element)n).getText() + institution);
            }

            n = ((Document)record).selectSingleNode("/doc/field[@name='state']");
            if (n == null) {
                ((Document)record).getRootElement().addElement("field")
                .addAttribute("name", "state")
                .addText("null");
            }
            int val = checkRecord((Document)record, i);

            setPrimaryInst((Document)record);

            if ((val & RECORD_PRESENT) > 0) {
                updated_records+=1;
                if ((val & RECORD_DIFFERENT_CONTRIBUTOR) > 0) {
                    updated_changed_records+=1;
                }
            } else {
                ((Document)record).getRootElement().addElement("field")
                .addAttribute("name","created_date_la")
                .addText(updated_date);
                new_records+=1;
            }
            document.getRootElement().add(((Document)record).getRootElement());
            i+=1;
        }
        postString(document.asXML());

        document = DocumentHelper.createDocument();
        Element delete_ele = document.addElement("delete");
for (Object record : records.getDeletedRecords()) {
            try {
                Pattern pattern = Pattern.compile("<\\s*identifier\\s*>(.*?)<\\s*/\\s*identifier\\s*>");
                Matcher matcher = pattern.matcher(((Document)record).asXML());
                matcher.find();

                Document solr_doc = getSolrDocument("oaiid:\"" + matcher.group(1) + "\" AND inst:\"" + institution + "\"");
                if (solr_doc == null || getSolrDocumentHits(solr_doc) == 0) {
                    deleted_not_present_cumu++;
                }
                callService(arrow_url + "/main/delete_document?oai=" + URLEncoder.encode(matcher.group(1), "UTF-8") + "&inst=" + URLEncoder.encode(institution, "UTF-8"));
            } catch (Exception e) {
                logger.locallog("Couldn't extract deleted identifier", getName());
            }
        }


        if (updated_changed_records > 0) {
            logger.locallog("Records Counts: Inserted: " + new_records + ", Updated: " + updated_records + " (including " + updated_changed_records + " new records that already existed for another contributor), Deleted: " + records.getDeletedRecords().size(), getName());
        } else {
            logger.locallog("Records Counts: Inserted: " + new_records + ", Updated: " + updated_records + ", Deleted: " + records.getDeletedRecords().size(), getName());
        }

        new_records_cumu += new_records;
        updated_records_cumu += updated_records;
        updated_changed_records_cumu += updated_changed_records;
        deleted_records_cumu += records.getDeletedRecords().size();
        records.setRecordsAdded(new_records);
        records.setDeletionsPerformed(records.getDeletedRecords().size());


        /*		if (((String)props.get("delete")).equals("notharvested")) {
        			logger.locallog("deleting some records now", "ArrowLoader");
        			String from = (String)props.get("harvestfrom");
        			String until = (String)props.get("harvestuntil");
         
        			if (from == null || from.equals("")) {
        				from = "*";
        			}else {
        				from = Long.toString(parseDate(from).getTime());
        			}
        			if (until == null || until.equals("")) {
        				until = "*";
        			}else {
        				until = Long.toString(parseDate(until).getTime());
        			}
         
        			postString("<delete><query>inst:\"" + institution + "\" AND oaidatestamp:[" + from + " TO " + until + "] AND -contributed_date:" + contributed_date + "</query></delete>");	
        		} */

        postString("<commit/>");
        callService(arrow_url + "/main/update_database");

        int records_in_collection = getSolrDocumentHits(getSolrDocument("*:*"));
        int records_for_contributor = getSolrDocumentHits(getSolrDocument("inst:\"" + cont.getName() + "\""));

        logger.locallog(Integer.toString(records_in_collection) + " records now in collection", "ArrowLoader");
        logger.locallog(Integer.toString(records_for_contributor) + " records from contributor", "ArrowLoader");


        DAOFactory.getDAOFactory().getcontributorDAO().setTotalRecords(cont.getContributorid(), records_in_collection, records_for_contributor);

        return processed_records;
    }
}



