package harvester.client.profileconfig;

import java.util.List;

public class CollectionProfileView {

	private List<Stage> steps;
	private String name;
	private String id;
	private String description;
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<Stage> getSteps() {
		return steps;
	}
	public void setSteps(List<Stage> steps) {
		this.steps = steps;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
	
}
