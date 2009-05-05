
package harvester.processor.steps;

import org.dom4j.*;
import com.Ostermiller.util.ExcelCSVParser;
import java.util.*;
import converter.operations.*;


/** Convert values in an xml record */
public class ConvertValue extends GenericStep {
//public class ConvertValue implements StagePluginInterface {

    public String getName() {
        return "Convert Value";
    }

    public Document processRecord(Document record, int position) throws Exception {
        String fieldXpath = (String)props.get("Field Name");
        String rules = (String)props.get("Rules");

        String[][] values = ExcelCSVParser.parse(rules);

        //LinkedList<HashMap<String, String>> check_fields = (LinkedList<HashMap<String, String>>) props.get("Conversion");

        if(rules == null) {
            return record;
        }

        List nodes = record.selectNodes(fieldXpath);

        for (Object node : nodes) {
            for(int i=0; i<values.length; i++) {
                String regexp = values[i][0];
                String new_expr = values[i][1];
                OpConvert op = new OpConvert(regexp, new_expr);
                //System.out.println(node);
                op.apply((Node)node);
                if (op.getMatched()) {				// don't match the same node multiple times.
                    break;
                }
            }
        }
        return record;
    }
}



