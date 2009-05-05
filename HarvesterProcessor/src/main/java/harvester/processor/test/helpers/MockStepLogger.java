package harvester.processor.test.helpers;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Node;

import harvester.processor.util.StepLogger;

public class MockStepLogger implements StepLogger {

	public int error_count = 0;
	
	public void error(String description, Throwable excp) {
		System.out.println(description);
		excp.printStackTrace();
	}

	//Copied from StepLoggerImpl
	public String getOAIIdentifier(Document data) {
		List<Node> cnodes = data.selectNodes("comment()");
		
		for(Node n : cnodes) {
			if(n.getText().startsWith("identifier"))
				return n.getText().substring("identifier=".length());			
		}
		
		return null;
	}

	public void info(String description) {
		System.out.println(description);
	}

	public void locallog(String msg, String classname) {
		System.out.println("Local Log: classname=" + classname + " msg=" + msg);
	}

	public void log(String description) {
		System.out.println(description);
	}

	public void log(int errorlevel, String description, String reason,
			Integer stepid, String data) {
		System.out.println("Err lvl=" + errorlevel + " Desc=" + description);
		System.out.println("DATA: " + data);
		
	}

	public void logfailedrecord(int errorlevel, String description,
			Document data, int position, String name, Integer stepid,
			int recordnumber) {
		error_count++;
		System.out.println("Failed Record: Err lvl=" + errorlevel + " Desc=" + description + " Pos=" + position + " Name=" + name + " Nbr=" + recordnumber);
		System.out.println("DATA: " + data);
	}

	public void logfailedrecord(String description, String reason,
			Integer stepid, String data) {
		error_count++;
		log(StepLogger.RECORD_ERROR, description, reason, stepid, data);	
	}

	public void logprop(String name, String value, Integer stepid) {
		System.out.println("PROPERTY: Name=" + name + " value=" + value);

	}

	public void logreport(String name, String value, Integer stepid) {
		System.out.println("REPORT: Name=" + name + " value=" + value);
	}

}
