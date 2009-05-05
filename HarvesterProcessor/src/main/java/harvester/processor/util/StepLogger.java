package harvester.processor.util;

import harvester.data.HarvestLog;

import org.dom4j.Document;
/**
 * Main Implementation is StepLoggerImpl, interface has been extracted out to make unit testing easier
 * @author adefazio
 *
 */
public interface StepLogger {

	public static final int INFO = HarvestLog.INFO;
	public static final int RECORD_ERROR = HarvestLog.RECORD_ERROR;
	public static final int STEP_ERROR = HarvestLog.STEP_ERROR;
	public static final int INIT_ERROR = HarvestLog.INIT_ERROR;
	public static final int PROP_INFO = HarvestLog.PROP_INFO;
	public static final int REPORT_INFO = HarvestLog.REPORT_INFO;

	/** identifier must be in a comment */
	public abstract String getOAIIdentifier(Document data);

	/**
	 * We have a specific format for logging failed records that this function conforms to.
	 * This will only log to the user visible logs if less then 100 failed records have been
	 * logged using this instance of the step logger object.
	 * @param errorlevel	one of the level enum
	 * @param description text to print as the reason
	 * @param data an optional dom4j xml document containing the failed record
	 * @param position the position in the pipeline of the stage that failed this record
	 * @param name the name of the step that failed the record
	 * @param recordnumber the number of the record in the harvest, (starting at zero, but we show user recordnumber+1)
	 */
	public abstract void logfailedrecord(int errorlevel, String description,
			Document data, int position, String name, Integer stepid, int recordnumber);

	/** logs to both the database and locally
	 * @param errorlevel one of the error level numbers in this class
	 * @param description a string to log
	 * @param data a data attachment, possibily null to also include in the database's version of the log
	 */
	public abstract void log(int errorlevel, String description, String reason, Integer stepid, String data);

	/** logs for a failed record, to both the database and locally, but less restrictive then the other log failed record.
	 * @param description a string to log
	 * @param data a data attachment, possibily null to also include in the database's version of the log
	 */
	public abstract void logfailedrecord(String description, String reason, Integer stepid, String data);
	
	/** log a property used in this harvest, will be show in a seperate section in the user interface then other log messages
	 * @param name property name
	 * @param value property value
	 */
	public abstract void logprop(String name, String value, Integer stepid);

	/** log a piece of info that won't show up to the user, but can be extracted for reporting later
	 * @param name property name
	 * @param value property value
	 */
	public abstract void logreport(String name, String value, Integer stepid);
	
	/**
	 * log just to the local log files
	 * @param msg the message
	 * @param classname the class name to report as the origin of the log message
	 */
	public abstract void locallog(String msg, String classname);

	/**
	 * log to both the database and locally, with log level INFO
	 * @param description the log message
	 */
	public abstract void log(String description);

	/**
	 * log to both the database and locally, with log level INFO
	 * @param description the log message
	 */
	public abstract void info(String description);

	/**
	 * Logs locally an error message and a stack trace
	 * @param description the msg to print to the log files.
	 * @param excp The exception object that contains the stack trace.
	 */
	public abstract void error(String description, Throwable excp);

}
