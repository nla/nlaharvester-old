package harvester.client.connconfig.actions;

import harvester.client.connconfig.StepParameterView;
import harvester.client.data.dao.DAOFactory;
import harvester.client.util.KeyValue;
import harvester.data.Contributor;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

//import org.apache.commons.collections.KeyValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;

import org.w3c.dom.*;
import org.xml.sax.InputSource;



/**
 * This class performs OAI based look ups to retrieve the sets and metadata prefixes supported
 * by a OAI repository, as well as its Date granularity. This is performed between the 2nd and 
 * 3rd steps of the connection settings wizard.
 */
public class OAIOptionProcessor implements StepOptionProcessor{

	protected final Log logger = LogFactory.getLog(getClass());
	
    public static final int DEFAULT_GRANULARITY = 0;
    public static final int LONG_GRANULARITY = 1;
    
    private static Map<Integer, List<KeyValue>> setDescriptions = Collections.synchronizedMap(new HashMap<Integer, List<KeyValue>>());
    
    private int setId;
    private int setDescId;
    
	public void setSetDescId(int setDescId) {
		this.setDescId = setDescId;
	}

	public void setSetId(int setId) {
		this.setId = setId;
	}
	
	public boolean process(List<StepParameterView> spv, DAOFactory daofactory, Contributor c)
    {

    	boolean success = true; //will be set to false at some point if something bad but recoverable occurs

    	logger.info("starting option processing for a OAI datasource");
    	//we currently have the baseurl in the spv,
    	//we then need to do a few requests to gather the data we need
    	//to fill out the options for the drop down boxes

    	clearDesc(c.getContributorid());
    	
    	//just fetch references to the parameterview objects we need first
    	StepParameterView baseurl = null;
    	StepParameterView mdp = null;			//metadata prefix
    	StepParameterView set = null;
    	StepParameterView encoding = null;


    	//-----------------------------xpath stuff -----------------------------
    	Document namespaceHolder = null;
    	try
    	{
	    	DocumentBuilderFactory factory = DocumentBuilderFactory
	    	.newInstance();
	    	factory.setNamespaceAware(true);
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	DOMImplementation impl = builder.getDOMImplementation();
	    	namespaceHolder = impl.createDocument(
	    			"http://www.oclc.org/research/software/oai/harvester",
	    			"harvester:namespaceHolder", null);
	    	namespaceHolder.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/",
	    			"xmlns:oai20", "http://www.openarchives.org/OAI/2.0/");
    	}
    	catch (Exception e)
    	{
    		logger.error("problem configuring xml stuff");
    		return false;
    	}
    	//-----------------------------------------------------------------------

    	//because its stored as a list of views, we have to do this
    	//we use string comparison because its easy and not that slow
    	for(StepParameterView sp : spv)
    	{
    		if(sp.getName().equals("Base URL"))
    			baseurl = sp;
    		else if(sp.getName().equals("Set"))
    			set = sp;
    		else if(sp.getName().equals("Metadata Prefix"))
    			mdp = sp;
    		else if (sp.getName().equals("Encoding"))
    			encoding = sp;
    	}
    	if(baseurl == null || mdp == null || set == null)
    		//Not all required fields found
    		return false;
    	if(encoding == null)
    	{
    		logger.info("null encoding");
    		return false;
    	} else if(encoding.getValue().equals("") || encoding.getValue().equals("autodetect"))
    	{
    		encoding = new StepParameterView();
    		encoding.setValue("UTF-8");
    	}
    	
    	//trim any whitespace off of the baseurl, as per a reported jira issue
    	baseurl.setValue(baseurl.getValue().trim());
    	
    	//set up the option stuff for the drop down
    	set.setType(StepParameterView.DROP_DOWN);    	
    	set.setOptions(new LinkedList<KeyValue>());
    	mdp.setType(StepParameterView.DROP_DOWN);
    	mdp.setOptions(new LinkedList<KeyValue>());

    	try
    	{

	    	logger.info("starting a ListSets command...");
	    	Document setdoc = getDocumentFromWS(baseurl.getValue() + "?verb=ListSets", encoding.getValue());
	
	    	//we need to check if we just received a noSetHierarchy error, which we need to handle
	    	XObject errorobject = XPathAPI.eval(setdoc, "/oai20:OAI-PMH/oai20:error/@code" , namespaceHolder.getDocumentElement());
	    	if(errorobject != null & errorobject.str().equals("noSetHierarchy"))
	    	{	    	
		    		logger.info("received a no set responce");
		    		//we don't want them to be able to enter a set, so set set to readonly
		    		set.setDescription("This repository does not support sets");
		    		set.setType(StepParameterView.READ_ONLY);
	    	}
	    	else
	    	{
		    	//we want a nodelist of sets, so basically just remove the wrapper around them
		    	NodeList setnl = XPathAPI.selectNodeList(setdoc, 
		    			"/oai20:OAI-PMH/oai20:ListSets/oai20:set" , namespaceHolder.getDocumentElement());
		    	logger.info("number of sets=" + setnl.getLength());
		    	set.getOptions().add(new KeyValue("", "			"));
		    	
		    	for(int i = 0; i < setnl.getLength(); i++)
		    	{
		    		KeyValue kv = new KeyValue();
		    		String name = XPathAPI.eval(setnl.item(i), "oai20:setName/text()", namespaceHolder.getDocumentElement()).toString();
		    		String spec = XPathAPI.eval(setnl.item(i), "oai20:setSpec/text()", namespaceHolder.getDocumentElement()).toString();
		    		if( spec.length() + name.length() > 100)
		    			kv.setValue((spec.length() > 50 ? spec.substring(0, 50) : spec) + " - " + (name.length() > 50 ? name.substring(0,50) : name));  
		    		else
		    			kv.setValue(spec + " - " + name);
		    		kv.setKey(spec);
		    		set.getOptions().add(kv);
		    		
		    		addDesc(c.getContributorid(), spec, name);
		    	}
		    	if(set.getOptions().size() == 1)
		    	{
		    		throw new Exception("unable to get sets");
		    	}
	    	}
	    	logger.info("fetched set list");
    	} catch (Exception e)
    	{
    		logger.error("problem fetching set information");
    		success = false;
    		set.setType(set.TEXT);	//if we don't get any set options, let them just type one in
    	}

    	try
    	{
    	
    	logger.info("starting a ListMetadataFormats command...");
    	Document metadoc = getDocumentFromWS(baseurl.getValue() + "?verb=ListMetadataFormats", encoding.getValue());

    	//we want a nodelist of of the metadataprefixes, which we will use for both the key and value of the Keyvalue type
    	NodeList metnl = XPathAPI.selectNodeList(metadoc, 
    			"/oai20:OAI-PMH/oai20:ListMetadataFormats/oai20:metadataFormat/oai20:metadataPrefix" ,
    			namespaceHolder.getDocumentElement());
    	logger.info("number of metadata prefixes=" + metnl.getLength());
    	for(int i=0; i < metnl.getLength(); i++)
    	{
    		String metadataprefix = metnl.item(i).getFirstChild().getNodeValue();
    		mdp.getOptions().add(new KeyValue(metadataprefix, metadataprefix));
    	}

    	if(mdp.getOptions().size() == 0)
    	{
    		throw new Exception("unable to get metadata prefixes");
    	}

    	logger.info("feteched metadataprefix list");

    	} catch (Exception e)
    	{
    		logger.error("problem fetching metadata prefixes");
    		mdp.setType(mdp.TEXT);
    		success = false;
    	}
    	
    	//logging stuff
    	for(StepParameterView s : spv)
    	{
    		if(s.getOptions() != null)
    		{
    			for(KeyValue kv : s.getOptions())
    				logger.info("key=" + kv.getKey() + " value=" + kv.getValue());
    		}
    	}

    	try
    	{
    		String granularity = null;
    		logger.info("starting a Identify command");
    		Document identdoc = getDocumentFromWS(baseurl.getValue() + "?verb=Identify", encoding.getValue());
    		
    		XObject xgranularity = XPathAPI.eval(identdoc, "/oai20:OAI-PMH/oai20:Identify/oai20:granularity/text()" ,
        			namespaceHolder.getDocumentElement());
    		if(xgranularity != null)
    			granularity = xgranularity.str();
    		if(granularity != null && granularity.equals("YYYY-MM-DDThh:mm:ssZ"))
    		{
    			logger.info("detected long granularity");
    			c.setGranularity(LONG_GRANULARITY);
    		}
    		else
    		{
    			logger.info("using short granularity");
    			c.setGranularity(DEFAULT_GRANULARITY);
    		}
    		
    	} catch (Exception e)
    	{
    		logger.error("can't fetch granularity info, using default");
    		c.setGranularity(DEFAULT_GRANULARITY);
    	}
    	
    	return success;
	}

	private Document getDocumentFromWS(String url, String encoding) throws Exception{
		//create the ListSets command to access there ws
		URL requesturl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
		conn.setRequestMethod("GET");
		conn.setReadTimeout(120000);
		conn.connect();
		if(conn.getResponseCode() != HttpURLConnection.HTTP_OK)
			throw new Exception("Unable to contact OAI web service");
		
        DOMParser parser = new DOMParser();

		InputStream in = conn.getInputStream();
		InputStreamReader isr = new InputStreamReader(in, encoding);
		InputSource source = new InputSource(isr);
		parser.parse(source);
		in.close();
	        
        conn.disconnect();
        logger.info("got xml document from ws");
        return parser.getDocument();
	}

	public int getHtype() {
		return 0;	//this is the OAI type
	}

	private void addDesc(Integer contributorid, String spec, String name ) {
		List<KeyValue> sets = setDescriptions.get(contributorid);
		sets.add(new KeyValue(spec, name));
	}
	
	private void clearDesc(Integer contributorid) {
		setDescriptions.put(contributorid, new LinkedList<KeyValue>());
	}
	
	public void postProcess(Map<Integer, String> parameters, DAOFactory daofactory, Contributor c) {

		String setSpec = parameters.get(setId);
		
		List<KeyValue> sets = setDescriptions.get(c.getContributorid());
		
		String description = null;
		
		for(KeyValue set : sets) {
			if(set.getKey().equals(setSpec))
				description = set.getValue();
		}
		
		parameters.put(setDescId, description);
	}
}
