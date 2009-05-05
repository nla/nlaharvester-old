package harvester.processor.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class validatorErrorHandler implements ErrorHandler {

	private StepLogger logger;
	
	public validatorErrorHandler(StepLogger logger)
	{
		this.logger = logger;
		logger.locallog("validator error handler initialized", "ValidatiorErrorHandler");
	}
	
    public void error(SAXParseException saxParseEx) throws SAXParseException {
        logger.info( "Error during validation: " +  saxParseEx.toString());
     }
     
     public void fatalError(SAXParseException saxParseEx) throws SAXParseException  {
        logger.info( "Fatal error during validation: " + saxParseEx.toString());
     }
     
     public void warning(SAXParseException saxParseEx) {
        logger.info("Warning during validation: " + saxParseEx.toString());
     }

}
