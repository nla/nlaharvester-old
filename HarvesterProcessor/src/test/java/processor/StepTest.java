package processor;

import java.util.*;
import org.junit.*;
import org.dom4j.*;
import harvester.processor.steps.*;
import harvester.processor.main.*;
import harvester.processor.test.helpers.*;
import harvester.processor.util.HarvestConnection;
import harvester.processor.util.OaiClient;
import harvester.processor.util.StepLogger;
import harvester.processor.util.StreamUtil;
import static org.junit.Assert.*;

public class StepTest {
	String record5 	=	"<doc>"
									+	"<field name=\"identifier\">http://www.google.com.au</field>"
									+ "<field name=\"author\">Tor Lattimore</field>"
									+ "<field name=\"author\">tor.lattimore@gmail.com</field>"
									+ "<field name=\"author\">Hutter, Marcus</field>"
									+ "<field name=\"author\">Marcus.Hutter@nicta.edu.au</field>"
									+ "<field name=\"author\">Scott H. Sanner (Scott.Sanner@anu.edu.au)</field>" 
									+ "<field name=\"type\">Report on NFL</field>"
									+ "<field name=\"description\">Comparison of NFL and Solmonoff Induction</field>"
									+ "<field name=\"title\">No Free Lunch</field>"
									+ "</doc>";

	String record4 	=	"<doc>"
									+	"<field name=\"identifier\">http://www.google.com.au</field>"
									+ "<field name=\"identifier\">http://www.yahoo.com</field>"
									+ "</doc>";

	String record3 = "<doc>"
								+ "<field name=\"title\">This title contains 5 words</field>"
								+ "</doc>";	

	String record2 = "<doc>"
								+ "<field name=\"title\">A dissertion on Foo</field>"
								+	"<field name=\"author\">Tor Lattimore</field>"
								+ "</doc>";	

	String record1 = "<doc>"
								+ "<field name=\"title\">A dissertion on Foo</field>"
								+	"<field name=\"author\">Tor Lattimore</field>"
								+ "<field name=\"author\">Finnian Lattimore</field>"
								+ "</doc>";	
	String response =  
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
		"<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">" + 
		" <responseDate>2002-06-01T19:20:30Z</responseDate> " + 
		" <request verb=\"ListRecords\" from=\"1998-01-15\" set=\"physics:hep\" metadataPrefix=\"oai_rfc1807\"> http://an.oa.org/OAI-script</request>" + 
		" <ListRecords>" + 
		"  <record>" + //normal record
		"    <header>" + 
		"      <identifier>oai:arXiv.org:hep-th/9901001</identifier>" + 
		"      <datestamp>1999-12-25</datestamp>" + 
		"      <setSpec>physics:hep</setSpec>" + 
		"      <setSpec>math</setSpec>" + 
		"    </header>" + 
		"    <metadata>" + 
		"     <rfc1807 xmlns=\"http://info.internet.isi.edu:80/in-notes/rfc/files/rfc1807.txt\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://info.internet.isi.edu:80/in-notes/rfc/files/rfc1807.txt http://www.openarchives.org/OAI/1.1/rfc1807.xsd\">" + 
		"        <bib-version>v2</bib-version>" + 
		"        <id>hep-th/9901001</id>" + 
		"        <entry>January 1, 1999</entry>" + 
		"        <title>Investigations of Radioactivity</title>" + 
		"        <author>Ernest Rutherford</author>" + 
		"        <date>March 30, 1999</date>" + 
		"     </rfc1807>" + 
		"    </metadata>" + 
		"  </record>" + 
		"  <record>" + //this record has inner record nodes, which used to cause problems
		"    <header>" + 
		"      <identifier>oai:arXiv.org:hep-th/9901001</identifier>" + 
		"      <datestamp>1999-12-25</datestamp>" + 
		"      <setSpec>physics:hep</setSpec>" + 
		"      <setSpec>math</setSpec>" + 
		"    </header>" + 
		"    <metadata>" + 
		"      <record>" + 
		"        <record>" + 
		"        </record>" + 		
		"      </record>" + 		
		"    </metadata>" + 		
		"  </record>" + 
		"  <record>" + 	//this records metadata section has no close tag!!!
		"    <header>" + 
		"      <identifier>oai:arXiv.org:hep-th/9901001</identifier>" + 
		"      <datestamp>1999-12-25</datestamp>" + 
		"      <setSpec>physics:hep</setSpec>" + 
		"      <setSpec>math</setSpec>" + 
		"    </header>" + 
		"    <metadata>" + 
		"  </record>" + 			
		"  <record>" + 	//deleted record
		"    <header status=\"deleted\">" + 
		"      <identifier>oai:arXiv.org:hep-th/9901007</identifier>" + 
		"      <datestamp>1999-12-21</datestamp>" + 
		"    </header>" + 
		"  </record>" + 
		" </ListRecords>" + 
		"</OAI-PMH>";
	
	String shortdoc = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\" ?><OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\"><responseDate>2008-11-05T03:51:29Z</responseDate><request metadataPrefix=\"marcxml\" verb=\"ListRecords\" from=\"2008-11-02T12:00:00Z\" set=\"Authority\">http://soa-test.nla.gov.au/apps/librariesaustralia-oai/OAIHandler</request><ListRecords>" + 
		"<resumptionToken completeListSize=\"20025\" cursor=\"1\">start=1001&amp;count=1000&amp;from=2008-11-02T12%3A00%3A00Z&amp;until=9999-12-31T23%3A59%3A59Z&amp;set=Authority&amp;metadataPrefix=marcxml</resumptionToken></ListRecords></OAI-PMH>";

	
	
	@Test
	public void CompositionStepTest() throws Exception {
		System.out.println("Composition step test");

		Document doc = DocumentHelper.parseText(record5);	

		HashMap<String, String> row;
		GenericStep step;

		// Convert names to standard 'LASTNAME, Firstname' format
		HashMap<String, Object> props = new HashMap<String, Object>();
		//LinkedList<HashMap<String, String>> check_fields = new LinkedList<HashMap<String, String>>();
		
		//row = new HashMap<String, String>();
	
		StringBuilder csv = new StringBuilder();
		
		csv.append("[^\\s]+@[^\\s]+,\n");
		csv.append("(.*),");
		csv.append(
				"\"#set ($name = $C.getName($g1))" + 
				"#if ($name.isValid())" + 
				"#set ($person = $D.getRootElement().addElement(\"\"person\"\"))" +
				"#set ($x = $person.addElement(\"\"firstname\"\").addText($name.getFirstName()))" +
				"#set ($x = $person.addElement(\"\"lastname\"\").addText($name.getLastName()))" +
				"#end" +
				"#set ($x = $E.detach())\""
				);

		props.put("Rules", csv.toString());
		props.put("Field Name", "doc/field[@name='author']");

		step = new ConvertValue();
		step.Initialise(props, new MockStepLogger(), new MockServletContext());

		doc = step.processRecord(doc, 0);
		System.out.println(doc.asXML());
	}

	@Test
	public void DeleteStepTest() throws Exception {
		System.out.println("Delete Step Test");
		
		Document doc = DocumentHelper.parseText(record1);	

		HashMap<String, Object> props = new HashMap<String, Object>();
		LinkedList<HashMap<String, String>> check_fields = new LinkedList<HashMap<String, String>>();

		HashMap<String, String> row = new HashMap<String, String>();
		row.put("Field Name", "/doc/field[@name='title']");
		row.put("Field Value", ".*");
		check_fields.add(row);

		row = new HashMap<String, String>();
		row.put("Field Name", "/doc/field[@name='author']");
		row.put("Field Value", ".*Tor.*");
		check_fields.add(row);

		props.put("Fields", check_fields);

		DeleteField step = new DeleteField();
		step.Initialise(props, new MockStepLogger(), new MockServletContext());
		doc = step.processRecord(doc, 0);
		
		assert(doc != null);
		assert(doc.selectSingleNode("/doc/field[@name='title']") == null);
		assert(doc.selectSingleNode("/doc") != null);
		assert(doc.selectSingleNode("/doc/field[@name='author']") != null);
		assert(doc.selectNodes("/doc/field[@name='author']").size() == 1);

	}

	@Test
	public void ConvertStepTest() throws Exception {
		System.out.println("Convert step test");

		Document doc = DocumentHelper.parseText(record2);	

		HashMap<String, Object> props = new HashMap<String, Object>();
		String data = "(.*?)\\s+(.*?)$,\"$g2.toUpperCase(), $g1.toLowerCase()\"";

		props.put("Rules", data);
		props.put("Field Name", "doc/field[@name='author']");

		ConvertValue step = new ConvertValue();
		step.Initialise(props, new MockStepLogger(), new MockServletContext());
		doc = step.processRecord(doc, 0);

		assert(doc != null);
		assert(doc.selectSingleNode("/doc/field[@name='author']") != null);
		assert(doc.selectSingleNode("/doc/field[@name='author']").getText().equals("LATTIMORE, tor"));
	}

	@Test
	public void SplitStepTest() throws Exception {
		System.out.println("Split step test");
		Document doc = DocumentHelper.parseText(record3);	

		HashMap<String, Object> props = new HashMap<String, Object>();
		LinkedList<HashMap<String, String>> check_fields = new LinkedList<HashMap<String, String>>();

		HashMap<String, String> row = new HashMap<String, String>();
		row.put("Field Name", "/doc/field[@name='title']");
		row.put("Delimiter", "\\s+");
		check_fields.add(row);
		props.put("Fields", check_fields);

		SplitField step = new SplitField();
		step.Initialise(props, new MockStepLogger(), new MockServletContext());
		doc = step.processRecord(doc, 0);
		assert(doc != null);
		assert(doc.selectNodes("/doc/field[@name='title']").size() == 5);

	}

	@Test
	public void CheckRepeatedTest() throws Exception {
		System.out.println("CheckRepeated step test");
		Document doc = DocumentHelper.parseText(record4);	

		HashMap<String, Object> props = new HashMap<String, Object>();
		LinkedList<HashMap<String, String>> check_fields = new LinkedList<HashMap<String, String>>();

		HashMap<String, String> row = new HashMap<String, String>();
		row.put("Field Name", "/doc/field[@name='identifier']");
		row.put("Match Value", "http.*");
		check_fields.add(row);
		props.put("Check Fields", check_fields);

		CheckRepeated step = new CheckRepeated();
		step.Initialise(props, new MockStepLogger(), new MockServletContext());
		doc = step.processRecord(doc, 0);
		assert(doc == null);
	}
	
	@Test
	public void OAIClientListRecordsTest() throws Exception {
		MockHarvestConnection hc = new MockHarvestConnection();
		hc.addRecord(response.getBytes());
		OaiClient client = new OaiClient("www.example.com", "physics:hep" , "oai_rfc1807", "1998-01-15", null, new MockStepLogger(), null, 0);
		client.setHarvestConnection(hc);
		String returned_response = client.getNext();
		//we expect no next after the first
		assert(!client.hasNext());
		assert(returned_response.equals(response));
	}
	
	@Test
	public void resumptionTokenTest() throws Exception {
		MockHarvestConnection hc = new MockHarvestConnection();
		hc.addRecord(shortdoc.getBytes());
		OaiClient client = new OaiClient("www.example.com", "physics:hep" , "oai_rfc1807", "1998-01-15", null, new MockStepLogger(), null, 0);
		client.setHarvestConnection(hc);
		client.getNext();

		assert(client.getCompleteListSize() == 20025);
		assert(client.hasNext());
		assert(client.getCursor() == 1);
	}
	
	@Test
	public void HarvesterThroughWSRecordTest() throws Exception {
		MockStepLogger logger = new MockStepLogger();
		MockHarvestConnection hc = new MockHarvestConnection();
		hc.addRecord(response.getBytes());
		HashMap<String, Object> props = new HashMap<String, Object>();
		
		props.put("Base URL", "www.example.com");
		props.put("Metadata Prefix", "oai_dc");
		props.put("stepid", 42);	//for logging		
		
		HarvesterThroughWS step = new HarvesterThroughWS();
		step.Initialise(props, logger, new MockServletContext());
		step.getOaiClient().setHarvestConnection(hc);	//get the data from local memory instead of web request
		
		Records rec = new Records();
		step.Process(rec);
		//the document we harvest will have two malformed record, one deleted and 1 normal
		assert(logger.error_count == 1);
		assert(rec.getTotalRecords() == 3);
		assert(rec.getCurrentrecords() == 2);
		assert(rec.getDeletedRecords().size() == 1);
		assert(rec.getRecordsinsource() == null);
		assert(!rec.isContinue_harvesting());
		
	}
	
	@Test
	public void WordBreakUrlTest() throws Exception {
		String url = "http://soa-test.nla.gov.au/apps/librariesaustralia-oai/OAIHandler?verb=ListRecords&resumptionToken=start%3D1001%26count%3D1000%26from%3D0001-01-01T00%253A00%253A00Z%26until%3D9999-12-31T23%253A59%253A59Z%26set%3DAuthority%26metadataPrefix%3Dmarcxml";
		String burl = OaiClient.AddWordBrakesToUrl(url);
		assert(burl.equals("http://soa-test.nla.gov.au/apps/librariesaustralia-oai/OAIHandler?verb=ListRecords&resumptionToken=s<wbr />tart%3D1001%26count%3D1000%26from%3D0001-01-01T00%253A00%253A00Z%26until%3D9999-12-31T23%253A59%253A<wbr />59Z%26set%3DAuthority%26metadataPrefix%3Dmarcxml"));
		//System.out.println("broken url:" + burl);
		String url2 = "http://export.arxiv.org/oai2?verb=ListRecords&metadataPrefix=oai_dc";
		assert(OaiClient.AddWordBrakesToUrl(url2).equals(url2));
	
	}
}
