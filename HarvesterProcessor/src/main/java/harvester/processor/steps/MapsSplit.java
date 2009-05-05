package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import javax.servlet.ServletContext;
import java.util.*;
import java.util.regex.*;


/** Delete a field from a xml record */
public class MapsSplit extends GenericStep {

    public String getName() {
        return "Maps Split";
    }

    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);
    }

	public boolean fixDates(Document doc) {
		Pattern pair = Pattern.compile(".*(\\d\\d\\d\\d).+(\\d\\d\\d\\d).*");
		Pattern single = Pattern.compile(".*(\\d\\d\\d\\d).*");
		HashMap map = new HashMap();
		map.put("eac", "http://jefferson.village.virginia.edu/eac");
		XPath parts = doc.createXPath("//eac:part[@type='date']");
		parts.setNamespaceURIs(map);

		XPath existdate = doc.createXPath("//eac:existdate[@scope='begin']");
		existdate.setNamespaceURIs(map);

		List<Element> nodes = parts.selectNodes(doc);
		logger.locallog(doc.asXML(), getName());
		logger.locallog("found " + nodes.size() + " nodes", getName());
		int found = 0;
		Branch parent = null;
		String start = null;
		for (Element node : nodes) {
			if (node.getParent() != parent) {
				if (found == 1) {
					parent.addElement("existdate").
						addAttribute("calendar", "gregorian").
						addAttribute("form", "openspan").
						addAttribute("normal", start).
						addAttribute("scope", "begin").
						addText(start);
				}

				start = null;
				found = 0;
			}
			parent = node.getParent();

			Matcher matcher = pair.matcher(node.getText());
			if (matcher.matches()) {
				found+=2;
				node.detach();
				parent.	addElement("existdate").
						addAttribute("calendar", "gregorian").
						addAttribute("form", "closedspan").
						addAttribute("normal", matcher.group(1) + "/" + matcher.group(2)).
						addAttribute("scope", "begin-end").
						addText(matcher.group(1) + "-" + matcher.group(2));
			}else {
				matcher = single.matcher(node.getText());
				if (matcher.matches()) {
					node.detach();
					found+=1;
					if (found > 2) {
						return false;
					}else if (found == 1) {
						start = matcher.group(1);
					}else if (found == 2) {
						parent.addElement("existdate").
							addAttribute("calendar", "gregorian").
							addAttribute("form", "closedspan").
							addAttribute("normal", start + "/" + matcher.group(1)).
							addAttribute("scope", "begin-end").
							addText(start + "-" + matcher.group(1));
					}
				}
			}
		}
		if (found == 1) {
			parent.	addElement("existdate").
				addAttribute("calendar", "gregorian").
				addAttribute("form", "openspan").
				addAttribute("normal", start).
				addAttribute("scope", "begin").
				addText(start);
		}
		return true;
	}

    public Records Process(Records records) throws Exception {
    	logger.locallog("processing", getName());

    	int total = 0;

    	Records processed_records = new Records(records);
    	for(Iterator<Object> itor = records.getRecords().iterator(); itor.hasNext();) {
    		Document record = (Document)itor.next();
    		List<Comment> comments = record.selectNodes("comment()");

    		for(Comment comment : comments)
    			record.remove(comment);

    		Pattern pattern = Pattern.compile("<eac .*?>(.*?)</eac>", Pattern.DOTALL);
    		Matcher matcher = pattern.matcher(record.asXML());
    		while (matcher.find()) {
    			total++;
    			Document doc = DocumentHelper.parseText(
    					"<eac xmlns=\"http://jefferson.village.virginia.edu/eac\">" + matcher.group(1) + "</eac>");
    			for(Comment n : comments)
    				doc.add(n);

				if (fixDates(doc)) {
	    			processed_records.addRecord(doc);
				}else {
	                logger.logfailedrecord(StepLogger.RECORD_ERROR,"More than two dates found." , doc, position, getName(), stepid,  total);
				}
    		}
    	}
    	processed_records.setTotalRecords(total);
    	return processed_records;

    }

    /* should never be called */
    public Document processRecord(Document record, int position) throws Exception {
    	return null;
    }

}



