package harvester.processor.steps;

import harvester.data.*;
import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import java.util.*;

import javax.servlet.ServletContext;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.dom4j.*;
import org.dom4j.io.*;


/**
 * Performs an xml transformation
 */
public class Translator extends GenericStep {

    private Transformer transformer;
    public String getName() {
        return "Translator";
    }


    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);

        //get the stylesheet name
        try {
            String ss = props.get("stylesheet").toString();
            Contributor contributor = (Contributor)props.get("contributor");
            Calendar calendar = Calendar.getInstance();

            logger.locallog("Using stylesheet: " + ss, getName());
            String folder = props.get("stylesheetfolder").toString();	//stored in the config file
            String xslt = "";
            String record = null;

            TransformerFactory factory = TransformerFactory.newInstance();

            transformer = factory.newTransformer(new StreamSource(servletContext.getRealPath(folder + ss)));

            transformer.setParameter("day", calendar.get(Calendar.DAY_OF_MONTH));
            transformer.setParameter("month", calendar.get(Calendar.MONTH));
            transformer.setParameter("year", calendar.get(Calendar.YEAR));
            transformer.setParameter("hour", calendar.get(Calendar.HOUR));
            transformer.setParameter("minute", calendar.get(Calendar.MINUTE));
            transformer.setParameter("second", calendar.get(Calendar.SECOND));
            transformer.setParameter("contributor", contributor.getName());
        } catch (NullPointerException e) {
            throw new Exception("Stylesheet was null");
        }

    }

    public Document processRecord(Document record, int position) throws Exception {
        //strip off comments
        List comments = record.selectNodes("comment()");
		for(Object comment : comments)
            record.remove((Node)comment);
		
		logger.locallog(record.asXML(), getName());
        DocumentSource source = new DocumentSource(record);
        DocumentResult result = new DocumentResult();
        transformer.transform( source, result );
        Document doc = result.getDocument();

        //add comments back on
		for(Object n : comments)
            doc.add((Node) n);
				logger.locallog(doc.asXML(), getName());
        return doc;
    }
}
