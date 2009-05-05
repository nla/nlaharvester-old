package harvester.processor.steps;

import harvester.data.Contributor;
import harvester.data.StepFile;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.util.StepLogger;

import java.io.StringReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;

public class XSLTTranslator extends GenericStep{

    private Transformer transformer;
    public String getName() {
        return "XSLTTranslator";
    }


    @SuppressWarnings("unchecked")
	public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);

        //get the stylesheet name
        try {
        	
            Contributor contributor = (Contributor)props.get("contributor");
            Calendar calendar = Calendar.getInstance();

            Integer fileid = Integer.valueOf(props.get("fileid").toString());
            StepFile file = null;
            
            List<StepFile> files = (List<StepFile>)props.get("stepFiles");
            for(StepFile f : files)
            	if(f.getFileid().equals(fileid))
            		file = f;
            
            String filedata = DAOFactory.getDAOFactory().getStepFileDAO().getFileData(fileid);
            
            logger.locallog("Using stylesheet:", getName());
            logger.locallog("fileid: " + file.getFileid(), getName());
            logger.locallog("Name: " + file.getFilename(), getName());
            logger.locallog("Desc: " + file.getDescription(), getName());

            TransformerFactory factory = TransformerFactory.newInstance();
                   
            //logger.info("data:" + filedata);

            transformer = factory.newTransformer(new StreamSource(new StringReader(filedata)));

            transformer.setParameter("day", calendar.get(Calendar.DAY_OF_MONTH));
            transformer.setParameter("month", calendar.get(Calendar.MONTH));
            transformer.setParameter("year", calendar.get(Calendar.YEAR));
            transformer.setParameter("hour", calendar.get(Calendar.HOUR));
            transformer.setParameter("minute", calendar.get(Calendar.MINUTE));
            transformer.setParameter("second", calendar.get(Calendar.SECOND));
            transformer.setParameter("contributor", contributor.getName());
            
        } catch (NullPointerException e) {
        	logger.error("translator", e);
            throw new Exception("Stylesheet was null");
        }

    }

    public Document processRecord(Document record, int position) throws Exception {
    	
    	//the comments allow us to keep track of a records identify even after its been translated and otherwise butchered
    	
        //strip off comments
        List comments = record.selectNodes("comment()");
        
				for(Object comment : comments)
	          record.remove((Node)comment);
        DocumentSource source = new DocumentSource(record);
        DocumentResult result = new DocumentResult();
        transformer.transform( source, result );
        Document doc = result.getDocument();

        //add comments back on
				for(Object n : comments)
		            doc.add((Node) n);
        return doc;
    }
	
}
