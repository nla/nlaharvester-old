package harvester.data;

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
@Table ( name = "contactselections")
public class ContactSelections  implements java.io.Serializable {

	private static final long serialVersionUID = -272052162212134376L;

	private Integer selectionid;

	private Contributor contributor;
	private CollectionContact contact;
	private int record;
	private int harvest;
	private int failure;
	private int success;
	private int businesstype;
	
	
	public static final int BUSINESS_CONTACT = ContributorContact.BUSINESS_CONTACT;
	public static final int TECHNICAL_CONTACT = ContributorContact.TECHNICAL_CONTACT;
	public static final int FROM_NLA = ContributorContact.FROM_NLA;
	public static final int FROM_OTHER = ContributorContact.FROM_OTHER;
	
	//For velocity
	public int BUSINESS_CONTACT() { return BUSINESS_CONTACT; }
	public int TECHNICAL_CONTACT() { return TECHNICAL_CONTACT; }
	public int FROM_NLA() { return FROM_NLA; }
	public int FROM_OTHER () { return FROM_OTHER ; }	
	
	
	
	public ContactSelections() {};

	public ContactSelections(int businesstype, CollectionContact contact,
			Contributor contributor, int failure, int harvest, int record,
			Integer selectionid, int success) {
		super();
		this.businesstype = businesstype;
		this.contact = contact;
		this.contributor = contributor;
		this.failure = failure;
		this.harvest = harvest;
		this.record = record;
		this.selectionid = selectionid;
		this.success = success;
	}
	@GenericGenerator(name = "generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = @Parameter(name = "sequence_name", value = "selections_seq"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name = "selectionid", nullable = false)
	public Integer getSelectionid() {
		return selectionid;
	}
	public void setSelectionid(Integer selectionid) {
		this.selectionid = selectionid;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contributorid", nullable = false)
	public Contributor getContributor() {
		return contributor;
	}
	public void setContributor(Contributor contributor) {
		this.contributor = contributor;
	}
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "contactid")
	public CollectionContact getContact() {
		return contact;
	}

	public void setContact(CollectionContact contact) {
		this.contact = contact;
	}
	
	@Column(name = "record")
	public int getRecord() {
		return record;
	}
	public void setRecord(int record) {
		this.record = record;
	}
	
	@Column(name = "harvest")
	public int getHarvest() {
		return harvest;
	}
	public void setHarvest(int harvest) {
		this.harvest = harvest;
	}
	
	@Column(name = "failure")
	public int getFailure() {
		return failure;
	}
	public void setFailure(int failure) {
		this.failure = failure;
	}
	
	@Column(name = "success")
	public int getSuccess() {
		return success;
	}
	public void setSuccess(int success) {
		this.success = success;
	}
	
	@Column(name = "businesstype")
	public int getBusinesstype() {
		return businesstype;
	}
	public void setBusinesstype(int businesstype) {
		this.businesstype = businesstype;
	}
	
	
}
