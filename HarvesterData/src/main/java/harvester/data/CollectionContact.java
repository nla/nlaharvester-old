package harvester.data;

// Generated Jul 9, 2008 11:14:11 AM by Hibernate Tools 3.2.1.GA

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * CollectionContact generated by hbm2java
 */
@Entity
@Table(name = "collectioncontact")
public class CollectionContact implements java.io.Serializable {

	private static final long serialVersionUID = -4409508005501090718L;
	
	private Integer contactid;
	private String name;
	private Collection collection;
	private int type;
	private String email;
	private String phone;
	private String jobtitle;
	private String note;
	private Set<ContactSelections> contactselections = new HashSet<ContactSelections>(0);

	public CollectionContact() {
	}

	@GenericGenerator(name = "generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = @Parameter(name = "sequence_name", value = "collectioncontact_seq"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name = "contactid", nullable = false)
	public Integer getContactid() {
		return this.contactid;
	}

	public void setContactid(Integer contactid) {
		this.contactid = contactid;
	}

	@Column(name = "name")
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "type")
	public int getType() {
		return this.type;
	}


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "collectionid", nullable = false)
	public Collection getCollection() {
		return collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Column(name = "email")
	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Column(name = "phone")
	public String getPhone() {
		return this.phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Column(name = "jobtitle")
	public String getJobtitle() {
		return this.jobtitle;
	}

	public void setJobtitle(String jobtitle) {
		this.jobtitle = jobtitle;
	}

	@Column(name = "note")
	public String getNote() {
		return this.note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="contact")
	//@JoinColumn(name = "contactid", updatable = false)
	@Cascade( { CascadeType.DELETE_ORPHAN, CascadeType.ALL })
	public Set<ContactSelections> getContactselections() {
		return contactselections;
	}
	public void setContactselections(Set<ContactSelections> contactselections) {
		this.contactselections = contactselections;
	}
	
	
}
