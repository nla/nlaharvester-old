package harvester.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;


@Entity
@Table(name = "harvestclusterdata")
public class HarvestClusterData  implements java.io.Serializable{

	private static final long serialVersionUID = -6708266035309818263L;
	private Integer harvestclusterid;
	private Integer harvestclusterdataid;
	private String term;
	private int count;
	
	@GenericGenerator(name = "generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = @Parameter(name = "sequence_name", value = "harvestclusterdata_seq"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name="harvestclusterdataid")
	public Integer getHarvestclusterdataid() {
		return harvestclusterdataid;
	}

	public HarvestClusterData() {
		
	}
	
	public void setHarvestclusterdataid(Integer harvestclusterdataid) {
		this.harvestclusterdataid = harvestclusterdataid;
	}
	
	@Column(name = "harvestclusterid", nullable = false)
	public Integer getHarvestclusterid() {
		return harvestclusterid;
	}
	
	public void setHarvestclusterid(Integer harvestclusterid) {
		this.harvestclusterid = harvestclusterid;
	}
	
	@Column(name = "term", nullable = false)
	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	@Column(name = "count", nullable = false)
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public HarvestClusterData(int count, Integer harvestclusterid, String term) {
		super();
		this.count = count;
		this.harvestclusterid = harvestclusterid;
		this.term = term;
	}
}
