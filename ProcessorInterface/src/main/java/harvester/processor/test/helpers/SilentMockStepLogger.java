
package harvester.processor.test.helpers;

import harvester.processor.util.StepLogger;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.Node;

public class SilentMockStepLogger implements StepLogger {

    private StringBuilder sb = new StringBuilder();

    public int error_count = 0;

    public String toString() {
        return sb.toString();
    }


    public void reset() {
        sb = new StringBuilder();
    }

    public void append(String msg) {
        sb.append(msg + "\n");
    }

    public void error(String description, Throwable excp) {
            append(description);
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
            append(description);
    }

    public void locallog(String msg, String classname) {
            append("Local Log: classname=" + classname + " msg=" + msg);
    }

    public void log(String description) {
            append(description);
    }

    public void log(int errorlevel, String description, String reason,
                    Integer stepid, String data) {
            append("Err lvl=" + errorlevel + " Desc=" + description);
            append("DATA: " + data);

    }

    public void logfailedrecord(int errorlevel, String description,
                    Document data, int position, String name, Integer stepid,
                    int recordnumber) {
            error_count++;
            append("Failed Record: Err lvl=" + errorlevel + " Desc=" + description + " Pos=" + position + " Name=" + name + " Nbr=" + recordnumber);
            append("DATA: " + data);
    }

    public void logfailedrecord(String description, String reason,
                    Integer stepid, String data) {
            error_count++;
            log(StepLogger.RECORD_ERROR, description, reason, stepid, data);
    }

    public void logprop(String name, String value, Integer stepid) {
            append("PROPERTY: Name=" + name + " value=" + value);

    }

    public void logreport(String name, String value, Integer stepid) {
            append("REPORT: Name=" + name + " value=" + value);
    }

}
