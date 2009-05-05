package harvester.client.profileconfig;

/**
 * holds the view's representation of a profile for use on the view profiles and edit profile screen.
 */
public class Stage {

	private Integer position;
	/** actually the name  */
	private String description;
	/** he description */
	private String functiondescription;
	/** usually says something like "XML" */
	private String input;
	/** usually says something like "XML" */	
	private String output; 
	private Integer psid;
	private Integer stepid;
	/** we don't show the edit buttons if this is true */
	private int restriction = 0;
	private int enabled = 1;
	
	
	
	public int getEnabled() {
		return enabled;
	}
	public void setEnabled(int enabled) {
		this.enabled = enabled;
	}
	public Integer getPosition() {
		return position;
	}
	public void setPosition(Integer position) {
		this.position = position;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getInput() {
		return input;
	}
	public void setInput(String input) {
		this.input = input;
	}
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public Integer getPsid() {
		return psid;
	}
	public void setPsid(Integer psid) {
		this.psid = psid;
	}
	public Integer getStepid() {
		return stepid;
	}
	public void setStepid(Integer stepid) {
		this.stepid = stepid;
	}
	public String getFunctiondescription() {
		return functiondescription;
	}
	public void setFunctiondescription(String functiondescription) {
		this.functiondescription = functiondescription;
	}
	public int getRestriction() {
		return restriction;
	}
	public void setRestriction(int restriction) {
		this.restriction = restriction;
	}

}
