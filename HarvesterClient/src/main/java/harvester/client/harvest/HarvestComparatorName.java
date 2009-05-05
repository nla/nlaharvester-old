package harvester.client.harvest;

import java.util.Comparator;

/**
 * Compares harvests by there name.
 *
 */
public class HarvestComparatorName implements Comparator<HarvestInfo>{

	public HarvestComparatorName()
	{
		
	}
	
	public int compare(HarvestInfo arg0, HarvestInfo arg1) {
		//System.out.println("using name comparator");
		return arg0.getContributorname().toLowerCase().compareTo(arg1.getContributorname().toLowerCase());
	}

}
