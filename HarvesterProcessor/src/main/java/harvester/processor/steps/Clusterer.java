package harvester.processor.steps;

import org.dom4j.*;

import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import harvester.processor.data.dao.*;
import harvester.data.*;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Calculate cluster information */
public class Clusterer extends GenericStep {

    private Integer stepid;
	
    class Cluster {
        String[] stop_words = {"the", "be", "to", "of", "and", "a", "in", "that", "have", "it", "for", "not", "on", "with", "he", "as", "you", "do",
                               "at", "this", "but", "his"};
        int max_size;
        
        HashMap<String,Integer> counts;
        public Cluster(int size) {
            counts = new HashMap();
            max_size = size;
        }
        boolean isStopWord(String text) {
        	for (String word : stop_words) {
                if (text.toLowerCase().equals(word)) {
                    return true;
                }
            }
            return false;
        }
        public void split_apply(String text) {
            if (size() > max_size) {
                return;
            }

            text = text.toLowerCase();
            String[] texts = text.split("\\s");

            for (String t : texts) {
                if (isStopWord(t)) {
                    continue;
                }
                apply(t);
            }
        }

        public void apply(String text) {
            if (size() > max_size) {
                return;
            }
            Integer val = 1;
            Integer entry = counts.get(text);
            if (entry != null) {
                val = entry + 1;
            }
            counts.put(text, val);
        }

        @SuppressWarnings("unchecked")
        public Map.Entry[] getSorted() {
            Set set = counts.entrySet();
            Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
            Arrays.sort(entries, new Comparator() {
                            public int compare(Object o1, Object o2) {
                                Object v1 = ((Map.Entry) o1).getValue();
                                Object v2 = ((Map.Entry) o2).getValue();
                                return ((Comparable) v2).compareTo(v1);
                            }
                        }
                       );
            return entries;
        }

        public int size() {
            return counts.size();
        }
    }

    private LinkedList<HashMap<String, String>> check_fields;
    private HashMap<String, Cluster> clusters;

    public String getName() {
        return "Clusterer";
    }

    @SuppressWarnings("unchecked")
    public void Dispose() {

    	for (Map.Entry cluster : clusters.entrySet()) {
            Map.Entry[] entries = ((Cluster)cluster.getValue()).getSorted();
            Set<HarvestClusterData> data = new HashSet<HarvestClusterData>();
            int count = 0;
            for (Map.Entry entry : entries) {
                logger.locallog((String)entry.getKey() + " : " +  (Integer)entry.getValue(), getName());
                count++;
                data.add(new HarvestClusterData((Integer)entry.getValue(), null, (String)entry.getKey()));
                if (count >= 100) {
                    break;
                }
            }
            logger.locallog((String)cluster.getKey(), getName());
            HarvestCluster hc = new HarvestCluster(data, null, Integer.valueOf((String)props.get("harvestid")), (String)cluster.getKey());
            DAOFactory.getDAOFactory().getHarvestClusterDAO().saveHarvestCluster(hc);
        }
    }

    @SuppressWarnings("unchecked")
    public void Initialise(HashMap<String, Object> props, StepLogger logger, ServletContext servletContext) throws Exception {
        super.Initialise(props, logger, servletContext);

        check_fields = (LinkedList<HashMap<String, String>>) props.get("Fields");
        clusters = new HashMap<String, Cluster>();

        for(HashMap<String, String> map : check_fields) {
            String fieldXpath = map.get("Field Name");
            clusters.put(fieldXpath, new Cluster(27000));
        }

		stepid = (Integer)props.get("stepid");
    }

    public Document processRecord(Document record, int position) {
    	for(HashMap<String, String> map : check_fields) {
            logger.locallog("map:" + map, getName());
            String fieldXpath = map.get("Field Name");
            String split = map.get("Split on Spaces");
            Cluster c = clusters.get(fieldXpath);
            List nodes = record.selectNodes(fieldXpath);
            for (Object node : nodes) {
                String text = ((Node)node).getText();
                if (text == null || text.equals("")) {
                    continue;
                }
                if (split == null || split.equals("0")) {
                    c.apply(text);
                } else {
                    c.split_apply(text);
                }
            }
            clusters.put(fieldXpath, c);
        }
        return record;
    }
}



