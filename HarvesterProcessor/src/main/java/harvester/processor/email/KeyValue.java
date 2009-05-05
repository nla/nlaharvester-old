package harvester.processor.email;



/**
 * A basic tuple of strings.
 * There are several places where a tuple style type are useful, so I wrote this simple class
 * There are probably better solutions already out there, but I only needed simple functionality
 */

public class KeyValue {
	private String key;
	private String value;
	
	public String toString() {
		return "key: " + key + " value: " + value;
	}
	
	public KeyValue(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public KeyValue()
	{
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}
