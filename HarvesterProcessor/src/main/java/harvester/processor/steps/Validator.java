package harvester.processor.steps;

import harvester.processor.util.StepLogger;
import harvester.processor.util.validatorErrorHandler;

import java.io.FileInputStream;
import java.util.HashMap;
import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.Verifier;
import org.iso_relax.verifier.VerifierFactory;
import org.iso_relax.verifier.VerifierHandler;

import org.dom4j.Document;
import org.dom4j.io.SAXWriter;

import org.xml.sax.InputSource;
import javax.servlet.ServletContext;

/** Performs a validation */
public class Validator extends GenericStep {

    protected Verifier verifier;

    private Integer stepid;
    
    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {

        super.Initialise(props, logger, servletContext);

        String ss = props.get("schema").toString();
        logger.locallog("using schema: " + ss, getName());
        String folder = props.get("schemafolder").toString();	//stored in the config file

        stepid = (Integer)props.get("stepid");
        
        // (1) use autodetection of schemas
        VerifierFactory factory = new com.sun.msv.verifier.jarv.TheFactoryImpl();
        InputSource is = new InputSource(new FileInputStream(servletContext.getRealPath(folder + ss)));
        is.setSystemId(ss);
        Schema schema = factory.compileSchema(is);

        // (2) configure a Vertifier
        verifier = schema.newVerifier();
        verifier.setErrorHandler(new validatorErrorHandler(logger));
    }

    public Document processRecord(Document record, int position) throws Exception {
        try {
            VerifierHandler handler = verifier.getVerifierHandler();
            SAXWriter writer = new SAXWriter( handler );
            writer.write(record);

            if(!handler.isValid()) {
                logger.logfailedrecord(StepLogger.RECORD_ERROR, "Did not validate", record, getPosition(), getName(), stepid, position);
                return null;
            }

        } catch (Exception e) {
            throw new Exception("Error with validation stylesheet");
        }
        return record;
    }

    public String getName() {
        return "Validator";
    }

}
