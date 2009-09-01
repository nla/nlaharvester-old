package harvester.processor.util;


import java.util.ArrayList;
import java.util.List;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;


public class MarcConverterTest extends XMLTestCase {

	protected List<String> testRecords = new ArrayList<String>();
	
	protected List<String> expectedRecords = new ArrayList<String>();

  @Override
  protected void setUp() throws Exception {
	  super.setUp();
	  
	  XMLUnit.setNormalizeWhitespace(true);
	  XMLUnit.setIgnoreWhitespace(true);
	  
	  // load the test records
	  Class<MarcConverterTest> clazz = MarcConverterTest.class;
	  Builder parser = new Builder();
	  Element doc = parser.build(clazz.getResourceAsStream(clazz.getSimpleName() + ".xml"), "").getRootElement();
	  
	  // extract the test records
	  Elements tests = doc.getChildElements("test");
	  for (int i = 0; i < tests.size(); i++) {
	  	Element test = tests.get(i);
	  	
	  	testRecords.add(test.getFirstChildElement("marc").getChildElements().get(0).toXML());
	  	expectedRecords.add(test.getFirstChildElement("expected").getChildElements().get(0).toXML());
	  }
  }
	
	public void testConversion() throws Exception {
		for (int i = 0; i < testRecords.size(); i++) {
			String marc = testRecords.get(i);
			String expected = expectedRecords.get(i);
			
			MarcConverter marcConverter = new MarcConverter();
			marcConverter.initialize(marc);
			
			assertTrue(marcConverter.isValid());
			
			String actual = marcConverter.convert();
			// assertEquals(expected, actual);
			
			DetailedDiff diff = new DetailedDiff(new Diff(expected, actual));
			assertTrue("Test record " + i + ":" + diff.toString(), diff.identical());
		}
	}
	
	protected Element parse(String xml) throws Exception {
		Builder parser = new Builder();
		
	  return parser.build(xml, "").getRootElement();
	}
	
}
