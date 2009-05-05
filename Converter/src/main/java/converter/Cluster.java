package converter;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * @author Tor Lattimore
 *
 * This class produces clusters based on input data. 
*/
public class Cluster {
	HashMap counts;

	/**
	 * The size is currently not used, but should represent the maximum number of unique terms that can be stored in
	 * the cluster.
	*/
	public Cluster(int size) {
		counts = new HashMap();
	}

	/**
	 * Add each word of a string to the cluster. (split by whitespace)
	*/
	public void split_apply(String text) {
		text = text.toLowerCase();
		String[] texts = text.split("\\s");
		for (String t : texts) {
			apply(t);
		}
	}	

	/**
	 * Add exactly 'text' to the cluster. (No splitting is done)
	*/
	public void apply(String text) {
		int val = 1;
		Object entry = counts.get(text);
		if (entry != null) {
			val = (Integer)entry + 1;
		}
		counts.put(text, val);
	}
	/**
	 * Returns a sorted list of words/counts in a Map.Entry iterator.
	*/
	public Map.Entry[] getSorted() {
		Set set = counts.entrySet();
		Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
		Arrays.sort(entries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Object v1 = ((Map.Entry) o1).getValue();
				Object v2 = ((Map.Entry) o2).getValue();
				return ((Comparable) v2).compareTo(v1);
			}
		});
		return entries;
	}
	/**
	 * Returns the number of unique terms in the cluster.
	*/
	public int size() {
		return counts.size();
	}
}
