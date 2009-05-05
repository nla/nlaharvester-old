package harvester.client.harvest;

import java.util.Set;

/** 
 * Represents one of the catogory of harvests to be shown in the harvest worktray. e.g. Scheduled harvests.
 * Holds information on the current sorting method and a isExpand property.
 */
public class HarvestCategory {

	/** Harvests to show under this catogory heading in the view */
	private Set<HarvestInfo> Harvests;
	
	private int defaultsort = SORT_BY_DATE;
	private boolean Expand = false;
	
	private int sort = defaultsort;
	private boolean viewall = false;
	private boolean overflowed = false;
	
	private int defaultdateordering = HarvestComparatorDate.REVERSE;
	
	public static final int SORT_BY_DATE = 1;	
	public static final int SORT_BY_REVERSE_DATE = -1;
	public static final int SORT_BY_NAME = 2;
	public static final int SORT_BY_STATUS = 3;
	
	public boolean isOverflowed() {
		return overflowed;
	}
	public void setOverflowed(boolean overflowed) {
		this.overflowed = overflowed;
	}
	public Set<HarvestInfo> getHarvests() {
		return Harvests;
	}
	public void setHarvests(Set<HarvestInfo> harvests) {
		Harvests = harvests;
	}
	public int getDefaultsort() {
		return defaultsort;
	}
	public void setDefaultsort(int defaultsort) {
		this.defaultsort = defaultsort;
	}
	public boolean isExpand() {
		return Expand;
	}
	public void setExpand(boolean expand) {
		Expand = expand;
	}
	public int getSort() {
		return sort;
	}
	public void setSort(int sort) {
		this.sort = sort;
	}
	public static int getSORT_BY_DATE() {
		return SORT_BY_DATE;
	}
	public static int getSORT_BY_NAME() {
		return SORT_BY_NAME;
	}
	public static int getSORT_BY_STATUS() {
		return SORT_BY_STATUS;
	}
	public int getDefaultdateordering() {
		return defaultdateordering;
	}
	public void setDefaultdateordering(int defaultdateordering) {
		this.defaultdateordering = defaultdateordering;
	}
	public boolean isViewall() {
		return viewall;
	}
	public void setViewall(boolean viewall) {
		this.viewall = viewall;
	}
	public static int getSORT_BY_REVERSE_DATE() {
		return SORT_BY_REVERSE_DATE;
	}
	
}
