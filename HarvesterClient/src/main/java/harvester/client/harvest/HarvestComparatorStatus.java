package harvester.client.harvest;

import java.util.Comparator;

/**
 * Compares harvests by their status
 */
public class HarvestComparatorStatus implements Comparator<HarvestInfo>{

	public int compare(HarvestInfo arg0, HarvestInfo arg1) {
		if(arg0.getStatus() == null || arg1.getStatus() == null)
			return arg0.getStatus() == null ? -1 : 1;
		else
		{
			int ct = arg0.getStatus().compareTo(arg1.getStatus());
			if(ct == 0)
				return arg0.getContributorname().toLowerCase().compareTo(arg1.getContributorname().toLowerCase());
			else
				return ct; 
		}
	}

}
