package harvester.client.connconfig;

import harvester.client.util.KeyValue;

import java.util.*;


/**
 * This class represents the views version of a steparameter
 * This is used in both the connection settings wizard and 
 * the profile editting wizard. Note that the connection settings
 * wizard does not used the nested fields at all
 * 
 */
public class StepParameterView {

	private String name;
	private int id;
	private String description;
	/** used to signify both readonly and required */
	private int editibility;
	private String value;
	private int type;
	/**currently the subtypes are used only in the profile wizards*/
	private int subtype;
	/**if this is a drop down box, the options are in this list*/
	private List<KeyValue> options;
	private String hiddenvalue;	//only used when read only hidden is used
	
	// if this class is being used in a profile wizard, we need these extra fields to hold data
	// regarding nested parameters. Maybe this should be in a sub class???
	/** recursivly nested parameters */
	private List<StepParameterView> nested;
	/** The values of the nested parameters. Note, only support one level of nesting. Values are in format
	 *  nestedfieldid.groupnumber -> value string */
	private HashMap<String, String> nestedvalues;
	private Integer numberofnested;
	
	public static final int TEXT = 1;
	public static final int DROP_DOWN = 2;
	public static final int CHECK_BOX = 3;
	public static final int READ_ONLY = 4;
	public static final int READ_ONLY_HIDDEN = 7;
	public static final int NESTED = 5;
	public static final int RADIO = 6;
	
	public static final int NORMAL = 0;
	public static final int REGEX = 1;
	public static final int XPATH = 2;
	
	public String toString(String indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append(indent + "name: " + name);
		sb.append(indent + "id: " + id);
		sb.append(indent + "desc:" + description);
		sb.append(indent + "value:" + value);
		sb.append(indent + "type:" + type);
		sb.append(indent + "subtype:" + type);
		if(options != null)
			sb.append(indent + "options:" + options.toString());
		sb.append("\n" + indent + "---NESTED---");
		if(nested != null)
			for(StepParameterView n : nested)
				sb.append(n.toString(indent+indent));
		if(nestedvalues != null)
			sb.append(indent + "nestedvalues:" + nestedvalues.toString());
		
		return sb.toString();
	}
	
	//used to identify the profile chosen when mixed in with database keys in a map
	public static final int PROFILE_ID = -123;
	
	public static int getNESTED() {
		return NESTED;
	}
	public static int getRADIO() {
		return RADIO;
	}
	public List<KeyValue> getOptions() {
		return options;
	}
	public void setOptions(List<KeyValue> options) {
		this.options = options;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public int getEditibility() {
		return editibility;
	}
	public void setEditibility(int editibility) {
		this.editibility = editibility;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public static int getTEXT() {
		return TEXT;
	}
	public static int getDROP_DOWN() {
		return DROP_DOWN;
	}
	public static int getCHECK_BOX() {
		return CHECK_BOX;
	}
	public static int getREAD_ONLY() {
		return READ_ONLY;
	}
	public List<StepParameterView> getNested() {
		return nested;
	}
	public void setNested(List<StepParameterView> nested) {
		this.nested = nested;
	}
	public HashMap<String, String> getNestedvalues() {
		return nestedvalues;
	}
	public void setNestedvalues(HashMap<String, String> nestedvalues) {
		this.nestedvalues = nestedvalues;
	}
	public Integer getNumberofnested() {
		return numberofnested;
	}
	public void setNumberofnested(Integer numberofnested) {
		this.numberofnested = numberofnested;
	}
	public int getSubtype() {
		return subtype;
	}
	public void setSubtype(int subtype) {
		this.subtype = subtype;
	}
	public static int getNORMAL() {
		return NORMAL;
	}
	public static int getREGEX() {
		return REGEX;
	}
	public static int getXPATH() {
		return XPATH;
	}
	public static int getREAD_ONLY_HIDDEN() {
		return READ_ONLY_HIDDEN;
	}
	public String getHiddenvalue() {
		return hiddenvalue;
	}
	public void setHiddenvalue(String hiddenvalue) {
		this.hiddenvalue = hiddenvalue;
	}

	
}
