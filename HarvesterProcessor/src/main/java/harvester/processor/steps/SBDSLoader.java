package harvester.processor.steps;


import harvester.data.Contributor;
import harvester.data.Profile;
import harvester.processor.exceptions.UnableToConnectException;
import harvester.processor.main.Records;
import harvester.processor.util.StepLogger;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.dom4j.Document;
import org.dom4j.Element;


public class SBDSLoader implements StagePluginInterface {
	
	/** a flag to indicate whether the object has been initialised or not */
	private boolean initialised = false;

	/** the configuration */
	protected HashMap<String, Object> props;

	/** a logger */
	protected StepLogger logger;

	/** the processing step position */
	protected int position;
	
	/** i have no idea what this is */
	private Integer stepId;

	/** the type of harvest: test or production. default is test */
	protected String type = "0";
	
	/** record statistics */
	protected int loaded = 0;
	protected int deleted = 0;

	/** the connection to the resource core */
	protected Socket socket = null;
	
	/** the channels for reading and writing to the socket */
	protected ReadableByteChannel rbc = null;
	protected WritableByteChannel wbc = null;
	
	/** the source for the records */
	int recordSource = 9999;
	
	/** a formatter for formatting log time stamps */
	protected DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSS");

	/**
	 * Get the name of this processing step. I assume it can contain spaces.
	 */
	public String getName() {
		return "SBDSLoader";
	}

	public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
		this.props = props;
		this.logger = logger;

		// get the properties
		this.type = (String) props.get("type");
		this.stepId = (Integer) props.get("stepid");
		
		// loader specific properties.
		String host = (String) props.get("host");
		String port = (String) props.get("port");
		
		Contributor contributor = (Contributor) props.get("contributor");
		this.recordSource = Integer.parseInt(contributor.getPlatform());

		if (!String.valueOf(Profile.TEST_PROFILE).equals(type)) {
			// try to connect if not a test harvest
			
			logger.locallog("Attempting to connect to " + host + ":" + port, getName());
	
			try {
				// open the connection
				socket = new Socket(host, Integer.parseInt(port));
				
	      // open channels for reading and writing
	      rbc = Channels.newChannel(socket.getInputStream());
	      wbc = Channels.newChannel(socket.getOutputStream());
			} catch (UnknownHostException e) {
				logger.locallog("Unknown host: " + host, getName());
				UnableToConnectException utce = new UnableToConnectException();
				utce.initCause(e);
				throw utce;
			} catch (IOException e) {
				logger.locallog("Couldn't get I/O for the connection to: " + host, getName());
				UnableToConnectException utce = new UnableToConnectException();
				utce.initCause(e);
				throw utce;
			}
		
		}

		initialised = true;
	}

	public Records Process(Records records) throws Exception {
		// check if the object has been initialised.
		if (!initialised) {
			logger.locallog("Test harvest, will not load into database.", getName());
			throw new RuntimeException(getName() + "has not been initialised.");
		}
		
		// check if this is a test or production harvest */
		if (String.valueOf(Profile.TEST_PROFILE).equals(type)) {
			logger.locallog("Test harvest, will not load into database.", getName());
			return records;
		}

		try {
			// process the harvested records
			logger.locallog("processing " + records.getRecords().size() + " records...", getName());
			
			int record_count = 0;
			for (Iterator<Object> i = records.getRecords().iterator(); i.hasNext(); ) {
				Document record = (Document) i.next();
				
				String identifier = record.valueOf("/record/header/identifier");
				Element rec = (Element) record.selectSingleNode("/record/metadata");
				if (rec != null && rec.elementIterator().hasNext()) {
					rec = (Element) rec.elementIterator().next();
				} else {
					rec = null;
				}
				if (!updateRecord(recordSource, identifier, "IM", rec)) {
					// try again
					if (!updateRecord(recordSource, identifier, "IM", rec)) {
						logger.logfailedrecord(StepLogger.RECORD_ERROR, "Unable to write record", record, getPosition(), getName(), stepId, record_count++);
						i.remove();
						
						continue;
					}
				}
				
				// record the record as processed
				records.setRecordsAdded(records.getRecordsAdded() + 1); 
				logger.locallog("record inserted into the database", getName());
				
				loaded++;
				record_count++;
			}
			
			// process the deleted records
			logger.locallog("processing " + records.getDeletedRecords().size() + " deleted records...", getName());
			
			for (Iterator<Object> i = records.getDeletedRecords().iterator(); i.hasNext(); ) {
				Document record = (Document) i.next();
				
				String identifier = record.valueOf("/record/header/identifier");
				Element rec = (Element) record.selectSingleNode("/record/metadata/child::node()");
				if (rec != null && rec.elementIterator().hasNext()) {
					rec = (Element) rec.elementIterator().next();
				} else {
					rec = null;
				}
				if (!updateRecord(recordSource, identifier, "DE", rec)) {
					// try again
					if (!updateRecord(recordSource, identifier, "DE", rec)) {
						logger.logfailedrecord(StepLogger.RECORD_ERROR, "Unable to write record", record, getPosition(), getName(), stepId, record_count++);
						i.remove();
					}
				}
				
				// record the record as processed
				records.setDeletionsPerformed(records.getDeletionsPerformed() + 1);
				logger.locallog("record deleted from the database", getName());
				
				deleted++;
				record_count++;
			}
		} finally {
			logger.locallog("processed: loaded: " + loaded + " deleted: " + deleted, getName());
		}

		return records;
	}

	public void Dispose() {
		// report the number of records processed
    logger.info("Records Count:<br />Loaded: " + loaded + "<br />Deleted: " + deleted);
		
		// try to close the connection
		try { if (rbc != null) rbc.close(); } catch (IOException e) { /* do nothing */ } finally { rbc = null; }
		try { if (wbc != null) wbc.close(); } catch (IOException e) { /* do nothing */ } finally { wbc = null; }
		try { if (socket != null) socket.close(); } catch (IOException e) { /* do nothing */ } finally { socket = null; }
		
		initialised = false;
	}

	/**
	 * Get the processing step position.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Set the processing step position.
	 */
	public void setPosition(int position) {
		this.position = position;
	}
	
	/**
	 * Update the supplied record in the repository
	 * @param recordSource		the source for the record
	 * @param recordId				the record identifier
	 * @param updateType			the type of update: IM add/update, DE delete
	 * @param record					the record to update
	 * @return true if the record updated successfully
	 * @throws IOException if there was some sort of network connection problem
	 */
	protected boolean updateRecord(int recordSource, String recordId, String updateType, Element record) throws IOException {
		// write log message
		writeBytes(wbc, getLogMessage(recordSource, recordId, updateType));
		
		// read acknowledgment 
		if (readBytes(rbc, 6)[1] != 0x02) {
			return false;
		}
		
		// write title record
		writeBytes(wbc, getTitleRecordMessage(recordSource, recordId, record != null ? record.asXML() : ""));
		
		// read acknowledgment
		if (readBytes(rbc, 6)[1] != 0x02) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Construct a CBS Pusher log message
	 * @param fileSetNumber  the source of the update
	 * @param recordNumber   the identifier of the record being updated
	 * @param updateType  	 the type of update
	 * @return the message formatted as a byte array
	 */
	protected ByteBuffer getLogMessage(int fileSetNumber, String recordNumber, String updateType) {
		ByteArrayOutputStream message = new ByteArrayOutputStream();
		
		try {
			
			message.write(formatString(df.format(new Date()), 17));  	// record stamp: 2005 02 09 10 13 57 75
			message.write(formatNumber(999, 2));											// updated by ILN
			message.write(formatString("", 11));											// updated by source code
			message.write(formatNumber(fileSetNumber, 2));						// title file set
			message.write(formatVarString(recordNumber)); 						// title identifier
			message.write(formatNumber(0, 2)); 												// title owner
			message.write(formatNumber(0, 1)); 												// title level: 0 record, 1 local, 2 holding
			message.write((byte) 'L'); 																// title status: H high priority,  L low priority
			message.write(formatString(updateType, 3)); 							// update type
			message.write(formatVarString("")); 											// old material tag
			message.write(formatVarString("")); 											// new material tag
			message.write(formatVarString("")); 											// used by ILNs
			message.write(formatNumber(0, 2)); 												// copy occurrence
			message.write(formatNumber(0, 4)); 												// copy number
			message.write(formatVarString("")); 											// old selection key
			message.write(formatVarString("")); 											// new selection key
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return generateMessageHeader(message, 0);
	}
	
	/**
	 * Construct a CBS Pusher title record message
	 * @param fileSetNumber  the source of the update
	 * @param recordId       the identifier of the record being updated
	 * @param xml  					 the record as an XML string
	 * @return the message formatted as a ByteBuffer
	 */
	protected ByteBuffer getTitleRecordMessage(int fileSetNumber, String recordId, String record) {
		ByteArrayOutputStream message = new ByteArrayOutputStream();
		
		try {
			
			message.write(formatNumber(fileSetNumber, 2));
			message.write(formatVarString(recordId));
			message.write(asUTF8(record));
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return generateMessageHeader(message, 1);
	}
	
	/**
	 * Generated a message header
	 * @param message		the message that requires a header
	 * @param type			the type of message: 0 or 17 is log record and 1 or 18 is title record
	 * @return the fully formed message
	 */
	protected ByteBuffer generateMessageHeader(ByteArrayOutputStream message, int type) {
		byte[] msg = message.toByteArray();

		ByteBuffer buf = ByteBuffer.allocate(msg.length + 6);
		
		buf.put(formatNumber(type, 2));
		buf.put(formatNumber(msg.length, 4));
		buf.put(msg);

		buf.flip();
		
		return buf;
	}
	
	/**
	 * Format a number into a byte array of length size using network byte order. 
	 * @param value		the number to format
	 * @param size		the number of bytes the value has to occupy
	 * @return the number formatted into a byte array
	 */
	protected byte[] formatNumber(String value, int size) {
		return formatNumber(Integer.parseInt(value), size);
	}
	
	/**
	 * Format a number into a byte array of length size using network byte order. 
	 * @param value		the number to format
	 * @param size		the number of bytes the value has to occupy
	 * @return the number formatted into a byte array
	 */
	protected byte[] formatNumber(int value, int size) {
    byte[] b = new byte[size];
    
    for (int i = 0; i < size; i++) {
        int offset = (b.length - 1 - i) * 8;
        b[i] = (byte) ((value >>> offset) & 0xFF);
    }
    
    return b;
	}
	
	/**
	 * Format a variable length string into Pusher varstring
	 * @param value		the String to format
	 * @return the string as a message length + byte array
	 */
	protected byte[] formatString(String value, int mlength) {
		// make sure the string is not too long
		if (value.length() > mlength - 1) {
			value = value.substring(0, mlength - 1);
		}
		
		// convert the value into bytes
		byte[] message = asUTF8(value);
		
		// check the length again as UTF-8 can convert a character to multiple bytes
		if (message.length >= mlength) {
			throw new RuntimeException("String too long: " + value + " (" + message.length + ") expected: " + (mlength - 1));
		}
		
		// create and initialize the output buffer
		byte[] out = new byte[mlength + 4];
		for (int i = 0; i < out.length; i++) out[i] = 0x0;
		
		// copy the message length and message to the output buffer
		byte[] length = formatNumber(mlength, 4);
		System.arraycopy(length, 0, out, 0, length.length);
		System.arraycopy(message, 0, out, length.length, message.length);
		
		return out;
	}
	
	/**
	 * Format a variable length string into Pusher varstring
	 * @param value		the String to format
	 * @return the string as a message length + byte array
	 */
	protected byte[] formatVarString(String value) {
		byte[] message = asUTF8(value);
		int mlength = message.length + 1;
		
		byte[] out = new byte[mlength + 4];
		for (int i = 0; i < out.length; i++) out[i] = 0x0;
		
		byte[] length = formatNumber(mlength, 4);
		System.arraycopy(length, 0, out, 0, length.length);
		System.arraycopy(message, 0, out, length.length, message.length);
		
		return out;
	}
	
	/**
	 * Format a String into a Pusher byte stream. the string will be UTF-8 encoded.
	 * @param value		the String to format
	 * @return the string as a message length + byte array
	 */
	protected byte[] formatByteStream(String value) {
		byte[] message = asUTF8(value);
		
		byte[] out = new byte[message.length + 4];
		
		byte[] length = formatNumber(message.length, 4);
		System.arraycopy(length, 0, out, 0, length.length);
		System.arraycopy(message, 0, out, length.length, message.length);
		
		return out;
	}
	
	protected byte[] asUTF8(String value) {
		if (value == null) {
			value = "";
		}
		
		try {
	    return value.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
    	throw new RuntimeException(e);
    }
	}
	
  /**
   * Write message to the repository
   * @param wbc   write pusher message to channel
   * @param wbuf  the message buffer to
   */
  protected void writeBytes(WritableByteChannel wbc, ByteBuffer wbuf) throws EOFException, IOException {

    int count = 0;
    do {
      int wrote = wbc.write(wbuf);
      if (wrote >= 0) {
        count += wrote;
      } else {
        throw new EOFException("Unable to write message.");
      }
    } while (count < wbuf.capacity());
  }
	
  /**
   * Read message from the repository
   * @param rbc  		read pusher message from channel
   * @param length  the expected length of the message
   * @return the bytes that were read
   */
  protected byte[] readBytes(ReadableByteChannel rbc, int length) throws EOFException, IOException {

    ByteBuffer buf = ByteBuffer.allocate(length);
    int count = rbc.read(buf);

    while (count < length) {
      int read = rbc.read(buf);
      if (read >= 0) {
        count += read;
      } else {
        throw new EOFException("Unable to read acknowledgement.");
      }
    }

    buf.flip();
    
    return buf.array();
  }

}
