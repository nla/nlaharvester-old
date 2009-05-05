package harvester.data;

// Generated Jul 9, 2008 11:14:11 AM by Hibernate Tools 3.2.1.GA

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

/**
 * ProfileStepParameter generated by hbm2java
 */
@Entity
@Table(name = "profilestepparameter")
public class ProfileStepParameter implements java.io.Serializable {

	private static final long serialVersionUID = -1400919636406400033L;
	
	private Integer profilestepparameterid;
	private ProfileStep pss;
	private ParameterInformation pis;
	
	//group list index is used for keeping track of multiple values for a nested parameter
	//consider a step that needs a list of field names and values to change those fields two
	//each field name goes with exactly one of theses values, so we need to link them somehow
	//basically, we call a bunch of parameters that a linked together as a group, and we distinguish
	//groups by there group number. For example, consider the input (("/foo", "bar"), ("fish", "sticks"))
	//suppose the profilestepparameter id for field name is 1, and value is two. Then this would be stored
	//as four rows of this class, as follows (id=1, value=/foo, group=1), (id=2, value=bar, group=1)
	//(id=1, value=fish, group=2), (id=2, value=sticks, group=2)
	private Integer grouplistindex;
	
	private String value;

	public ProfileStepParameter() {
	}

	public Object clone()
	{
		try
		{
			ProfileStepParameter p  = new ProfileStepParameter();
			p.setProfilestepparameterid(this.getProfilestepparameterid());
			p.setPss(this.getPss());
			p.setPis(this.getPis());
			p.setGrouplistindex(this.getGrouplistindex());
			p.setValue(this.getValue());
			return p;
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		ProfileStepParameter p = (ProfileStepParameter) obj;
		
		if(p.getProfilestepparameterid() != null && p.getProfilestepparameterid().equals(profilestepparameterid))
			return true;
		
		//logger.info("is pss equal=" + (pss.equals(p.getPss()) ? "true" : "false"));
		if(pss.equals(p.getPss()) && pis.getPiid().equals(p.getPis().getPiid()) && grouplistindex.equals(p.getGrouplistindex()))
			return true;
		return false;
	}
	
	public int hashCode()
	{
		int result = 7;
		result = 29*result + pss.hashCode();
		result = 31* result + pis.hashCode();
		if(grouplistindex != null)
			result = 37*result + grouplistindex.hashCode();
		return result;
	}

	public ProfileStepParameter(ProfileStep pss) {
		this.pss = pss;
	}

	public ProfileStepParameter(ProfileStep pss, ParameterInformation pis,
			Integer grouplistindex, String value) {
		this.pss = pss;
		this.pis = pis;
		this.grouplistindex = grouplistindex;
		this.value = value;
	}

	@GenericGenerator(name = "generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = @Parameter(name = "sequence_name", value = "profilestepparameter_seq"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name = "profilestepparameterid", nullable = false)
	public Integer getProfilestepparameterid() {
		return this.profilestepparameterid;
	}

	public void setProfilestepparameterid(Integer profilestepparameterid) {
		this.profilestepparameterid = profilestepparameterid;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "psid", nullable = false)
	public ProfileStep getPss() {
		return this.pss;
	}

	public void setPss(ProfileStep pss) {
		this.pss = pss;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "piid")
	public ParameterInformation getPis() {
		return this.pis;
	}

	public void setPis(ParameterInformation pis) {
		this.pis = pis;
	}

	@Column(name = "grouplistindex")
	public Integer getGrouplistindex() {
		return this.grouplistindex;
	}

	public void setGrouplistindex(Integer grouplistindex) {
		this.grouplistindex = grouplistindex;
	}

	@Column(name = "value")
	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
