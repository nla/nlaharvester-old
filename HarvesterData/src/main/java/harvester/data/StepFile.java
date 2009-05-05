package harvester.data;

import java.sql.Blob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "stepfile")
public class StepFile {

	private Integer fileid;
	private Integer stepid;
	private String filename;
	private String description;
	private Blob data;
	
	public StepFile(){};
	
	public StepFile(Blob data, String description, Integer fileid,
			String filename, Integer stepid) {
		super();
		this.data = data;
		this.description = description;
		this.fileid = fileid;
		this.filename = filename;
		this.stepid = stepid;
	}
	
	@GenericGenerator(name = "generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = @Parameter(name = "sequence_name", value = "stepfile_seq"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name = "fileid", nullable = false)
	public Integer getFileid() {
		return fileid;
	}
	public void setFileid(Integer fileid) {
		this.fileid = fileid;
	}
	
	@Column(name = "stepid")
	public Integer getStepid() {
		return stepid;
	}
	public void setStepid(Integer stepid) {
		this.stepid = stepid;
	}
	
	@Column(name = "filename")
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	@Column(name = "description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Column(name = "data")
	public Blob getData() {
		return data;
	}
	public void setData(Blob data) {
		this.data = data;
	}
	
	
	
}
