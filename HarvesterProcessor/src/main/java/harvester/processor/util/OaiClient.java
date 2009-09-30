package harvester.processor.util;

import harvester.processor.exceptions.*;
import harvester.processor.main.Controller;

import java.util.regex.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;


public class OaiClient {
	StepLogger logger;
	protected HashMap<Integer, String> statuscodes = new HashMap<Integer, String>();
	private HarvestConnection con;
	
	private Controller tc;
	
	String forced_encoding;
	
	String url;
	String set;
	String metadata_prefix;
	String from;
	String until;
	String oai_id;
	String request;
	Integer stepid;
	
	////////some constants, public for unit testing perposes
	int redirects = 0;	//counts the number of redirects, so we don't get stuck in a infinite redirect loop
	public int MAX_REDIRECTS = 10;
	public int SHORT_RETRY = 120;	//2 minutes
	public int MAX_RETRY_TIME = 30*60; //30 minutes
	public int MAX_RETRIES = 3;
	
	
	Integer complete_list_size = null;
	Integer cursor = null;

	String resumption_token = null;

	boolean has_next = true;

	private void OaiClientInit() {
		con = new HarvestConnectionImpl();
		addStatusCodes();
	}

	/* create an OaiClient for GetRecord verb */
	public OaiClient(Controller tc, String url, String oai_id, String metadata_prefix, 
					 StepLogger logger, String forced_encoding, Integer stepid) {
		OaiClientInit();
		this.forced_encoding = forced_encoding;
		this.logger = logger;
		this.url = url;
		this.oai_id = oai_id;
		this.metadata_prefix = metadata_prefix;
		this.stepid = stepid;
		this.tc = tc;
	}

	/* create an OaiClient suitable for ListRecords verb */
	public OaiClient(Controller tc, String url, String set, String metadata_prefix, 
					 String from, String until, StepLogger logger, String forced_encoding, Integer stepid) {
		OaiClientInit();
		this.forced_encoding = forced_encoding;
		this.logger = logger;
		this.url = url;
		this.set = set;
		this.metadata_prefix = metadata_prefix;
		this.from = from;
		this.until = until;
		this.stepid = stepid;
		this.tc = tc;
		oai_id = null;
	}
	public String getName() {
		return "OaiClient";
	}
	public String getRequest() {
		return request;
	}

	public Integer getCompleteListSize() {
		return complete_list_size;
	}

	public void setHarvestConnection(HarvestConnection newcon) {
		con = newcon;
	}
	
	public Integer getCursor() {
		return cursor;
	}

	//TODO: turn this off when css3 support is widespread...
	public static String AddWordBrakesToUrl(String url) {
		StringBuilder sb = new StringBuilder();
		//every 100 characters add a <wbr/>
		
		for(int i = 0; i < url.length(); i += 100) {
			int nextchunk = (url.length() - i < 100) ? url.length() : i + 100;
			sb.append(url.substring(i, nextchunk));
			if(nextchunk != url.length())
				sb.append("<wbr />");
		}
		
		return sb.toString(); 
	}
	
	public byte[] makeRequest(String request) throws RedirectException, AttemptRetryException, ConnectionException, InterruptedException  {

		/*  - According to the spec, a repository that returns a 503 response can also specify a "Retry-After" header. 
		 * 	  If it doesn't we should use the default retry.
		    - A 302,301 error should not do any retrying! They can use this to specify a redirect. If they do specify
		      a redirect url(in "Location" header) use that and continue as normal, otherwise throw an error message.
		      In general, a 307 is a temporary redirect, whereas for a 301 we should user log a stern message saying to change the 
		      actual harvest url in the client for future harvests.
		    - Anything other then a 2xx, 3xx (and maybe 1xx) response is an error, and should be logged with its Textual description, and
		      retried if the above conditions don't disallow it.
		      ALL reschedulings should be logged to the user logs. See log mockups.
		    - We need to make sure we log to the user when a timeout occurs!
		 */
		
		ByteArrayOutputStream rawout = new ByteArrayOutputStream();		
		int numLocalRetries = 0;
		Integer responseCode = null;
		InputStream responseStream = null;
		
		//tell the user what we are doing
		logger.info("Fetching: <a href=\"" + request + "\">" + OaiClient.AddWordBrakesToUrl(request) + "</a>");
		
		while(numLocalRetries < MAX_RETRIES) {	
			
			tc.yield();	// This is a good spot for harvests to stop at.
			
			try {
				responseCode = con.connect(request);
				logger.locallog("Response Code: " + responseCode + " : " + getStatusStringFromCode(responseCode), getName());
				if(responseCode < 300)
					responseStream = con.getInputStream();
			} catch(IOException e) {
				logger.error("error while trying to connect to or get the input stream from the repository", e);
				responseCode = null;
			}
			
			if(responseCode == null || responseCode > 300) {	//is this something unusual like an error?
				
				int retryInSeconds = SHORT_RETRY;
				
				if(responseCode != null) {
					
					if(responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {	//503
						//if the retry after field they send us is less then 30 minutes, we use it
						//otherwise we fail the harvest. If they send us no retry field, we use our default one
						String retryGivenStr = con.getHeaderField("Retry-After");
						if(retryGivenStr != null) {
							logger.locallog("Received Retry-After header: " + retryGivenStr, getName());
							int retryGiven = Integer.MAX_VALUE;
							try {
								retryGiven = Integer.parseInt(retryGivenStr);
							} catch (NumberFormatException e) { }
							if(retryGiven > MAX_RETRY_TIME) {
								logger.info("Bad Retry-After header, or too far in the future. Retry-After: " + retryGivenStr);
								throw new AttemptRetryException();	//fail for now, retry much later
							} else
								retryInSeconds = retryGiven;
						}
					}
					
					if(responseCode < 400) {	//some sort of redirect
						
						String redirectUrl = con.getHeaderField("Location");
						
						logger.locallog("got some sort of redirect", getName());
						logger.locallog("got redirect url: " + redirectUrl, getName());
						
						//if they don't give as a redirect url, we just treat this as a regular error message.
						if(redirectUrl != null) {
							//unfortunately, there is some ambiguity in what the redirect codes are and what they mean.
							//this is closest to the OAI specs recommendations
//							if(responseCode == 307)	//temp redirect
//								logger.info("Repository has been temporarily moved(307 response code), automatically redirecting to:\n" + redirectUrl);
//							if(responseCode == 302)
//								logger.info("Repository gave us a 302 FOUND response code, automatically redirecting to:\n" + redirectUrl);
//							if(responseCode == HttpURLConnection.HTTP_MOVED_PERM)	//301
//								logger.info("Repository has been Permanently moved(301 response code), automatically redirecting to:\n" + redirectUrl);
							//any other redirect code is likely to be some sort of load balancing.

							logger.info("Unable to connect<br/>Response Code: " + responseCode + " : " + statuscodes.get(responseCode)
									+ "<br/>Redirecting to: " + redirectUrl);
							
							throw new RedirectException(null, redirectUrl);	//try again imediately					
						}	

					}
					
					logger.log(StepLogger.STEP_ERROR, 
					    "Unable to connect<br/>Response Code: " + responseCode + " : " + statuscodes.get(responseCode)
						+ (numLocalRetries == MAX_RETRIES-1 ? "" : "<br/>Retrying in " + retryInSeconds + " seconds")
						, responseCode + " : " + statuscodes.get(responseCode) , stepid, null);
					
				} else {
					logger.log(StepLogger.STEP_ERROR, "Unable to contact repository" 
							+ (numLocalRetries == MAX_RETRIES-1 ? "" : "<br/>Retrying in " + retryInSeconds + " seconds")
							, "Unable to contact repository", stepid, null);
				}
				
				if(numLocalRetries != MAX_RETRIES-1) 	//don't bother sleeping if were going to exit straight after	
					Thread.sleep(retryInSeconds * 1000);	//must be given in milliseconds
					
				numLocalRetries++;
				continue;
			}
			
			try {
				String contentEncoding = con.getHeaderField("Content-Encoding");
				if ("compress".equals(contentEncoding)) {
					logger.locallog("content is ZIP compressed", getName());
					ZipInputStream zis = new ZipInputStream(con.getInputStream());
					zis.getNextEntry();
					responseStream = zis;
				} else if ("gzip".equals(contentEncoding)) {
					logger.locallog("content is GZIP compressed", getName());
					responseStream = new GZIPInputStream(con.getInputStream());					
				} else if ("deflate".equals(contentEncoding)) {
					logger.locallog("content is deflate compressed", getName());
					responseStream = new InflaterInputStream(con.getInputStream());
				}
				

				byte[] b = new byte[4096];
				for (int n; (n = responseStream.read(b)) != -1;) {
					rawout.write(b, 0, n);
				}
				break;
			} catch (IOException e) {
				logger.error("error with given input stream from repository", e);
			}			
		}
		
		if(numLocalRetries >= MAX_RETRIES) {
			logger.locallog("Retried " + numLocalRetries + " times, giving up", getName());
			throw new AttemptRetryException();			
		}
		
		return rawout.toByteArray();
	}
	
	public void checkErrors(String response) throws OaiException {
		Pattern pattern = Pattern.compile("<\\s*error\\s*code=\"(.*?)\"\\s*>(.*?)<\\s*/\\s*error\\s*>", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(response);

		if (matcher.find()) {
			throw new OaiException(matcher.group(2), matcher.group(1));
		}
	}

	/* check to see if a resumption token exists. If so then update it otherwise set the has_next variable to false */
	public void updateResumptionToken(String response) {
		Pattern pattern = Pattern.compile("<\\s*resumptionToken(.*?)>(.*?)</resumptionToken>", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(response);

		if (matcher.find() && matcher.group(2) != null) {
			if ("".equals(matcher.group(2).trim()) ) {
				logger.log(StepLogger.STEP_ERROR,"Received a empty resumption token, which is not conformant with the OAI spec.",
						"Received a empty resumption token", stepid, null);
				has_next = false;
				return;
			}
			String attributes = matcher.group(1);
			resumption_token = matcher.group(2);
			//required if they have &amp; symbols in the resumption token
			resumption_token = org.apache.commons.lang.StringEscapeUtils.unescapeXml(resumption_token);
			
			pattern = Pattern.compile("\\s*(.*?)\\s*=\\s*\"(.*?)\"");
			matcher = pattern.matcher(attributes);
			while (matcher.find()) {
				String name = matcher.group(1);
				String val = matcher.group(2);
				if (name.equals("cursor")) {
					cursor = Integer.valueOf(val);
				} else if (name.equals("completeListSize")) {
					complete_list_size = Integer.valueOf(val);
				}
			}
		} else {
			has_next = false;
		}

	}

	/* returns the next string of data */
	public String getNext() throws ConnectionException, AttemptRetryException, Exception {
		
		if(redirects >= MAX_REDIRECTS) {
			//stuck in a redirect loop
			logger.log(StepLogger.STEP_ERROR, "Redirected " + MAX_REDIRECTS + " times without success, probably stuck in a redirect loop.", "Redirect loop", stepid, null);
			throw new Exception();
		}
		try {
				
			if (!hasNext()) {
				return null;
			}
	
			if (oai_id != null) {
				byte[] data = makeRequest(url + "?verb=GetRecord&metadataPrefix=" + 
								URLEncoder.encode(metadata_prefix, "UTF-8") + "&identifier=" + oai_id);
				has_next = false;
				String response = decode(data);
				checkErrors(response);
				return response;
			}
	
	
			String request = url + "?verb=ListRecords";
			if (resumption_token == null) {
				request += "&metadataPrefix=" + URLEncoder.encode(metadata_prefix, "UTF-8");
				if (set != null) {
					request += "&set=" + URLEncoder.encode(set, "UTF-8");				
				}
				//valid from,until fields won't need url encoding
				if (from != null) {
					request += "&from="+from;
				}
				if (until != null) {
					request += "&until="+until;
				}
			} else {
				request += "&resumptionToken=" + URLEncoder.encode(resumption_token, "UTF-8");
			}
	
			this.request = request;
			byte[] data = makeRequest(request);
			String response = decode(data);
	
			checkErrors(response);
			updateResumptionToken(response);
	
			logger.locallog("resumption token: " + resumption_token, getName());
	
			return response;
			
		} catch (RedirectException e) {
			//try again with the new url.
			url = e.getUrl();
			if(url.contains("?")) {
				//strip off the GET parameters
				url = url.substring(0, url.indexOf('?'));
			}

			redirects++;
			logger.locallog("get redirect exception, num redirects now " + redirects, getName());
			return getNext();
		}
		
	}

	public String decode(byte[] data) {
		
		logger.locallog("response was " + data.length + " bytes long", getName());
		
		String response = "";
		try {
			
			if (forced_encoding == null || "autodetect".equals(forced_encoding)) {
				logger.locallog("Autodetecting encoding", getName());
				String encoding = Encoder.getEncoding(data);
				if (encoding == null) {
					logger.locallog("Failed to autodetect encoding. Using UTF-8", getName());
					encoding = "UTF-8";
				}else {
					logger.locallog("Encoding using autodetected encoding " + encoding, getName());
				}
				response = new String(data, encoding);
			}else {
				logger.locallog("Encoding using forced encoding " + forced_encoding, getName());
				response = new String(data, forced_encoding);
			}
			
		} catch (Exception e) {
			logger.locallog("failed to encode document. Document not re-encoded", getName());
			try {response = new String(data, "UTF-8"); } catch (Exception e2) {}		
		}
		return response;
	}
	
	public boolean hasNext() {
		return has_next;
	}
	
	public String getStatusStringFromCode(int code) {
		return statuscodes.get(code);
	}
	
	private void addStatusCodes() {
		statuscodes.put(0, "Network Error");
		statuscodes.put(100, "Continue");
		statuscodes.put(200, "OK");
		statuscodes.put(301, "Permanent Redirect");
		statuscodes.put(302, "Found");
		statuscodes.put(307, "Moved Temporarily");
		statuscodes.put(400, "Bad request");
		statuscodes.put(401, "Unauthorized");
		statuscodes.put(402, "Payment required");
		statuscodes.put(403, "Forbidden");
		statuscodes.put(404, "Page not found");
		statuscodes.put(405, "Method Not Allowed");
		statuscodes.put(406, "Not Acceptable");
		statuscodes.put(407, "Proxy Authentication Required");
		statuscodes.put(408, "Request Timeout");
		statuscodes.put(409, "Conflict");
		statuscodes.put(410, "Gone");
		statuscodes.put(411, "Length Required");
		statuscodes.put(412, "Precondition Failed");
		statuscodes.put(413, "Request Entity Too Large");
		statuscodes.put(414, "Request-URI Too Long");
		statuscodes.put(415, "Unsupported Media Type");
		statuscodes.put(416, "Requested Range Not satisifable");
		statuscodes.put(417, "Expectation Failed");
		statuscodes.put(500, "Internal Server error");
		statuscodes.put(501, "Not Implemented");
		statuscodes.put(502, "Bad Gateway");
		statuscodes.put(503, "Service Unavailable");
		statuscodes.put(504, "Gateway Timeout");
		statuscodes.put(999, "Undefined Error");
		statuscodes.put(990, "Blocked by robots.txt");		
	}
	
}
