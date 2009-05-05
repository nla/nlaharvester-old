package harvester.processor.steps;

import org.dom4j.*;
import harvester.processor.util.StepLogger;

/** Delete a field from a xml record */
public class DCToEAC extends GenericStep {
    public String getName() {
        return "DCToEAC";
    }

    public Document processRecord(Document record, int record_position) throws Exception {

        harvester.processor.util.DCToEAC converter = new harvester.processor.util.DCToEAC();
        String after_conversion = null;
        converter.initialize(record);

        try {
            after_conversion = converter.convert();
        } catch (Exception e) {
            logger.error("error during converter", e);	//logs stack trace
            logger.logfailedrecord(StepLogger.RECORD_ERROR,"Converter failed to transform record." , record, position, getName(), stepid, record_position);
            return null;
        }

        try {
            record = DocumentHelper.parseText(after_conversion); //converter.convert());
        } catch (Exception e) {
            logger.error("error parsing xml from converter", e);	//logs stack trace
            logger.logfailedrecord(StepLogger.RECORD_ERROR,"Converter did not produce valid XML." , record, position, getName(), stepid,  record_position);
            return null;
        }
        return record;

    }
}

