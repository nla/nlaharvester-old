package harvester.data;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;


@Entity
@Table(name = "report")
public class Report {

	private Integer reportid;
	private Contributor contributor;
	private Collection collection;
	private int type;
	private Date timestamp;
	private Blob data;
	private Date startdate;
	private Date enddate;
	
	public static final int REJECTED_RECORDS = 0;
	public static final int HARVEST_DATES = 1;
	public static final int SCHEDULING = 2;
	public static final int CONTRIBUTORS = 3;
	public static final int RECORDS_BY_COLLECTION = 4;
	public static final int HARVEST_ERRORS = 5;
	
	public Report(Contributor contributor, Collection collection, Blob data, Integer reportid,
			Date timestamp, int type) {
		super();
		this.collection = collection;
		this.contributor = contributor;
		this.data = data;
		this.reportid = reportid;
		this.timestamp = timestamp;
		this.type = type;
	}

	public Report() {
	}

	@GenericGenerator(name = "generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = @Parameter(name = "sequence_name", value = "report_seq"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name = "reportid", nullable = false)
	public Integer getReportid() {
		return reportid;
	}

	public void setReportid(Integer reportid) {
		this.reportid = reportid;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contributorid")
	public Contributor getContributor() {
		return contributor;
	}

	public void setContributor(Contributor contributor) {
		this.contributor = contributor;
	}

	@Column(name="type")
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Column(name="timestamp")
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Column(name="data")
	public Blob getData() {
		return data;
	}

	public void setData(Blob data) {
		this.data = data;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "collectionid", nullable = false)
	public Collection getCollection() {
		return this.collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	@Column(name="startdate")
	public Date getStartdate() {
		return startdate;
	}

	public void setStartdate(Date startdate) {
		this.startdate = startdate;
	}

	@Column(name="enddate")
	public Date getEnddate() {
		return enddate;
	}

	public void setEnddate(Date enddate) {
		this.enddate = enddate;
	}

}
