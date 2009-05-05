package harvester.processor.util;

import harvester.data.*;

import java.util.Comparator;

/** compares pipeline stages by there position */
public class pipelinestageComparator implements Comparator<ProfileStep> {

	public pipelinestageComparator()
	{
		
	}
	
	public int compare(ProfileStep arg0, ProfileStep arg1) {
		return Integer.valueOf(arg0.getPosition())
				.compareTo(Integer.valueOf(arg1.getPosition()));
	}

}
