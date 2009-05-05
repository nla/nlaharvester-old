package harvester.client.profileconfig;

import harvester.data.ProfileStep;

import java.util.Comparator;

/**
 * used for sorting pipelinestages by profileutil. Basically just sorts by position for use 
 * by the view.
 */
public class PipelineStageComparator implements Comparator<ProfileStep> {

	public PipelineStageComparator()
	{
		
	}
	
	public int compare(ProfileStep arg0, ProfileStep arg1) {
		return Integer.valueOf(arg0.getPosition())
				.compareTo(Integer.valueOf(arg1.getPosition()));
	}

}
