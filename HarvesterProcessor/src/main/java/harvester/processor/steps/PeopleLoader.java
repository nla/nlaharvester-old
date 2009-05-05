package harvester.processor.steps;

import org.dom4j.*;

import harvester.data.Profile;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.main.*;
import harvester.processor.util.EscapeHTML;
import harvester.processor.util.StepLogger;
import harvester.processor.util.StreamUtil;

import javax.servlet.ServletContext;

import java.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import harvester.data.Contributor;


/** PeopleLoader passes records to the identity service. 
*/
public class PeopleLoader implements StagePluginInterface {
	private int position;
	private StepLogger logger;
	private String identity_service_record_req_url;
	private String identity_url;
	protected HashMap<String, Object> props;
	String type;
	int stepid;
	int pending = 0;
	int inserted = 0;
	int updated = 0;
	int deleted = 0;
	int deleted_not_found = 0;
	String contributor_id;
	boolean double_encode_oaiids = true;

	public String getName() {
		return "PeopleLoader";
	}

	public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
		this.stepid = (Integer)props.get("stepid");
		this.identity_url = (String)props.get("identityurl");
        this.type = (String) props.get("type");
        String doubleEncode = (String)props.get("doubleEncodeOaiids");
        if("false".equals(doubleEncode))
        	double_encode_oaiids = false;
        this.logger = logger;
		this.props = props;
		
		this.identity_service_record_req_url = identity_url + "unmatched/record/{oaiid}?name=harvester&note=";
		
		Contributor contributor = (Contributor)props.get("contributor");
		contributor_id = Integer.toString(contributor.getContributorid());
		
	}

	public Records Process(Records records) throws Exception {
		if(String.valueOf(Profile.TEST_PROFILE).equals(type)) {
			logger.locallog("Test harvest, will not load into PEPO.", getName());
			return records;
		}
				
		logger.locallog("processing " + records.getRecords().size() + " records...", getName());

		HttpClient httpclient = new HttpClient();	//httpclient is sort of a connection manager for http requests
		int record_count = 0;
		
		for(Iterator<Object> itor = records.getRecords().iterator(); itor.hasNext();) {
			Document record = (Document)itor.next();
			
			String xml = record.asXML();
			
			String oaiid = EscapeHTML.forURL(logger.getOAIIdentifier(record));
			if(double_encode_oaiids)
				oaiid = EscapeHTML.forURL(oaiid);

			String url = identity_service_record_req_url.replaceAll("\\{oaiid\\}", oaiid);

			url+="&contributor_id=" + contributor_id;

			PutMethod put = new PutMethod(url);		
			RequestEntity entity = new StringRequestEntity(xml, "application/xml", "UTF-8");
			put.setRequestEntity(entity);
			
			try {
				
				httpclient.executeMethod(put);
				String response = StreamUtil.slurp(put.getResponseBodyAsStream());	//assumes UTF-8
				
				try {
					Document doc = DocumentHelper.parseText(response);

					String status = doc.selectSingleNode("response/status").getText();
					
					if( "INSERTED".equals(status.trim())) {
						inserted++;
						records.setRecordsAdded(records.getRecordsAdded()+1); //if this is a INSERTED record we need to increment our added counter
						logger.locallog("record inserted into identity service", getName());
					}
					if ("FAILED".equals(status.trim())) {
						String reason = doc.selectSingleNode("response/reason").getText();
						logger.locallog("identity service failed record url=" + url, getName());
						logger.logfailedrecord(StepLogger.RECORD_ERROR, reason,
								record, getPosition(), getName(), stepid, record_count);
						itor.remove();
					}
					if ("PENDING".equals(status.trim())) {
						pending++;
						logger.locallog("record pending in identity service", getName());
					}					
					if ("UPDATED".equals(status.trim())) {
						updated++;
						logger.locallog("record updated in identity service", getName());
					}

				} catch (Exception e) {
					logger.error("could not parse response for url: " + url, e);
					logger.locallog(response, getName());
					logger.logfailedrecord(StepLogger.RECORD_ERROR,"Could not parse response from identity service", 
							record, getPosition(), getName(), stepid, record_count);
					itor.remove();
				}
			} catch (Exception e) {
				logger.error("Exception in People Loader", e);	
				//TODO: we should probably have a retry algorithm like in arrow loader(httpclient supports custom error handlers...)
                logger.log(StepLogger.STEP_ERROR, "Problem sending data to identity service.", "Problem sending data to identity service" , stepid, null);
			} finally {
				// Release current connection to the connection pool once you are done
				put.releaseConnection();
			}
			record_count++;
		}
		//////////////////////////////////////////////////////////////////////////////////////////
		/////////////////deleted records
		for(Iterator<Object> itor = records.getDeletedRecords().iterator(); itor.hasNext();) {
			Document record = (Document)itor.next();
			
			String oaiid = EscapeHTML.forURL(logger.getOAIIdentifier(record));
			if(double_encode_oaiids)
				oaiid = EscapeHTML.forURL(oaiid);
		
			String url = identity_service_record_req_url.replaceAll("\\{oaiid\\}", oaiid);
			
			DeleteMethod del = new DeleteMethod(url);
			
			try {	// we log less for errors for delete requests
				
				httpclient.executeMethod(del);
				String response = StreamUtil.slurp(del.getResponseBodyAsStream());	//assumes UTF-8
				
				try {
					logger.locallog(response, getName());
					Document doc = DocumentHelper.parseText(response);

					String status = doc.selectSingleNode("response/status").getText();
					
					if( "DELETED".equals(status.trim())) {
						deleted++;
						records.setDeletionsPerformed(records.getDeletionsPerformed()+1);
					}
					if( "NOTFOUND".equals(status.trim())) {
						deleted_not_found++;
					}
					if( "FAILED".equals(status.trim())) {
						logger.locallog("deletion failed, url = " + url, getName());
					}
					//failed deletions just fall through silently
				} catch (Exception e) {
					logger.error("could not parse response: ", e);
					logger.locallog(response, getName());
				}
			} catch (Exception e) {
				logger.error("Exception in People Loader", e);	
				//TODO: we should probably have a retry algorithm like in arrow loader...
                logger.log(StepLogger.STEP_ERROR, "Problem sending data to identity service.", "Problem sending delete request to identity service" , stepid, null);
			} finally {
				// Release current connection to the connection pool once you are done
				del.releaseConnection();
			}
			
		}

		return records;
	}

	public void Dispose() {
		if(String.valueOf(Profile.PRODUCTION_PROFILE).equals(type)) {
			
			logger.locallog("fetching record counts", getName());
			//if there are problems processing either of these requests, we just leave the numbers as is
			Integer records_in_collection = getNumRecords(COLLECTION_RECORDS);
			Integer records_for_contributor = getNumRecords(CONTRIBUTOR_RECORDS);
			int contributor_id_int = ((Contributor)props.get("contributor")).getContributorid();
			if(records_in_collection != null && records_for_contributor != null) {
				try {
					DAOFactory.getDAOFactory().getcontributorDAO().setTotalRecords(contributor_id_int, records_in_collection, records_for_contributor);
				} catch (Exception e) {
					logger.error("error setting total records", e);
				}
			}
			
	        logger.info("Records Count:<br />Inserted: " + inserted + "<br />Updated: " + updated + "<br />Pending: " + pending
	        		+ "<br />Deleted: " + deleted + "<br />Deleted (that were not found): " + deleted_not_found);
		}
	}
	
	private static final int COLLECTION_RECORDS = 0;
	private static final int CONTRIBUTOR_RECORDS = 1;
	
	/**
	 * Retrieves the number of records in the collection, optionally filtered by contributor id.
	 * @param source_type either COLLECTION_RECORDS or CONTRIBUTOR_RECORDS;
	 * @return number of records
	 */
	public Integer getNumRecords(int source_type) {
		HttpClient httpclient = new HttpClient();

		String url = identity_url + "record";
		if(source_type == CONTRIBUTOR_RECORDS)
			url += "/?contributor_id=" + contributor_id;
		
		GetMethod get = new GetMethod(url);
		try {
			httpclient.executeMethod(get);
		} catch (Exception e) {
			logger.error("failed to execute method", e);
			return null;
		}

		try {
			String response = StreamUtil.slurp(get.getResponseBodyAsStream());	//assumes UTF-8
			Document doc = DocumentHelper.parseText(response);
			
			Node node = doc.selectSingleNode("//report/@total");
			logger.locallog(node.getText(), getName());
			return Integer.parseInt(node.getText());
		}catch (Exception e) {
			logger.error("error getting response", e);
			return null;
		}
	}
	
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
}


