package harvester.data;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;


@Entity
@Table(name = "harvestcluster")
public class HarvestCluster  implements java.io.Serializable{

	private static final long serialVersionUID = -5498957669751382397L;
	private Integer harvestclusterid;
	private Integer harvestid;
	private String xpath;
	private Set<HarvestClusterData> data = new HashSet<HarvestClusterData>(0);

	@GenericGenerator(name = "generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = @Parameter(name = "sequence_name", value = "harvestcluster_seq"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name = "harvestclusterid", nullable = false)
	public Integer getHarvestclusterid() {
		return harvestclusterid;
	}
	
	public HarvestCluster() {
		
	}
	
	public HarvestCluster(Set<HarvestClusterData> data, Integer harvestclusterid,
			Integer harvestid, String xpath) {
		super();
		this.data = data;
		this.harvestclusterid = harvestclusterid;
		this.harvestid = harvestid;
		this.xpath = xpath;
	}
	
	public void setHarvestclusterid(Integer harvestclusterid) {
		this.harvestclusterid = harvestclusterid;
	}
	
	@Column(name = "harvestid")
	public Integer getHarvestid() {
		return harvestid;
	}
	public void setHarvestid(Integer harvestid) {
		this.harvestid = harvestid;
	}
	
	@Column(name = "xpath")
	public String getXpath() {
		return xpath;
	}
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "harvestclusterid", updatable = false)
	@Cascade( { CascadeType.ALL, CascadeType.DELETE_ORPHAN})
	public Set<HarvestClusterData> getData() {
		return data;
	}

	public void setData(Set<HarvestClusterData> data) {
		this.data = data;
	}
	
}
