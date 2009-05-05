package harvester.client.harvest;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * A container object for a list of HarvestCatogories, so that we can easily pass the list to the view.
 */
public class HarvestNameModel {

	/** The names of categories that are in use. Filled in in the constructor */
	private LinkedList<String> categoryNames = new LinkedList<String>();
	
	/** Map from category name to A harvest category object */
	private HashMap<String, HarvestCategory> categories = new HashMap<String, HarvestCategory>();

	public LinkedList<String> getCategoryNames() {
		return categoryNames;
	}

	public void setCategoryNames(LinkedList<String> catogoryNames) {
		categoryNames = catogoryNames;
	}

	public HashMap<String, HarvestCategory> getCategories() {
		return categories;
	}

	public void setCategories(HashMap<String, HarvestCategory> catogories) {
		categories = catogories;
	}

	public HarvestNameModel()
	{
		categoryNames.add("TestHarvests");
		categoryNames.add("UnScheduled");
		categoryNames.add("Monitored");
		categoryNames.add("Unsuccessful");
		categoryNames.add("RecentSuccessful");
		
		for(String name : categoryNames)
		{
			categories.put(name, new HarvestCategory());
		}
		
		//because scheduled has the opposite date ordering, we put it on last
		
		categoryNames.add("Scheduled");
		HarvestCategory hc = new HarvestCategory();
		hc.setDefaultdateordering(HarvestComparatorDate.NORMAL);
		hc.setDefaultsort(HarvestCategory.SORT_BY_REVERSE_DATE);
		hc.setSort(HarvestCategory.SORT_BY_REVERSE_DATE);
		categories.put("Scheduled", hc);
	}

}
