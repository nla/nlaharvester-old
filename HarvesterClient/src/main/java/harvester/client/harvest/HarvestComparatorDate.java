package harvester.client.harvest;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Compares harvests by there date fields
 */
public class HarvestComparatorDate implements Comparator<HarvestInfo>{
	
	protected final Log logger = LogFactory.getLog(getClass());
	public static final int NORMAL = 0;
	public static final int REVERSE = 1;
	
	private boolean normalordering = true;
	private Comparator<HarvestInfo> fallbackcomp;
	
	public HarvestComparatorDate(int ordering)
	{
		if(ordering == NORMAL)
			normalordering = true;
		else
			normalordering = false;
		fallbackcomp = new HarvestComparatorName();
	}
	
	public int compare(HarvestInfo arg0, HarvestInfo arg1) {
		//System.out.println("arg0=" + arg0.getContributorid() + " arg1=" + arg1.getContributorid());
		
		if(arg0.getTimedate() == null && arg1.getTimedate() == null)
			return fallbackcomp.compare(arg0, arg1);
		else if(arg0.getTimedate() == null || arg1.getTimedate() == null)
			return arg0.getTimedate() == null ? -1 : 1;
		else {
			int comp = arg0.getTimedate().compareTo(arg1.getTimedate());
			if(comp == 0)
				comp = fallbackcomp.compare(arg0, arg1);
			if(normalordering)
				return comp;
			else
				return -comp;
		}
	}

}
