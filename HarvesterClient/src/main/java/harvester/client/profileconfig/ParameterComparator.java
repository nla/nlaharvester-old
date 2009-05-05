package harvester.client.profileconfig;

import harvester.data.*;

import java.util.Comparator;


/**
 * Used for sorting profile step parameters by the profileutil class.
 * profileutil uses a treeset sorted using this to remove holes in the group number numbering
 */
public class ParameterComparator implements Comparator<ProfileStepParameter> {

	public ParameterComparator()
	{
		
	}

	public int compare(ProfileStepParameter arg0,
			ProfileStepParameter arg1) {
		if(arg0.getGrouplistindex().equals(arg1.getGrouplistindex()))	
		{
			if(arg0.getPis().getPiid().equals(arg1.getPis().getPiid()))
				return 0;
			return arg0.getPis().getPiid() > arg1.getPis().getPiid() ? 1 : -1;
		}
		return arg0.getGrouplistindex() > arg1.getGrouplistindex() ? 1 : -1;
	}
	

}