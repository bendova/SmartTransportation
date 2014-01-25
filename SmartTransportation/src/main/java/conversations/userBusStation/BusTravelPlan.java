package conversations.userBusStation;

import java.util.List;

import uk.ac.imperial.presage2.util.location.Location;

public class BusTravelPlan implements IBusTravelPlan
{
	private List<Location> mPathToStart;
	private List<Location> mPathFromFinalList;
	
	public BusTravelPlan(List<Location> pathToStartBusStop,
			List<Location> pathFromFinalBusStop)
	{
		assert(pathToStartBusStop != null);
		assert(pathFromFinalBusStop != null);
		
		mPathToStart = pathToStartBusStop;
		mPathFromFinalList = pathFromFinalBusStop;
	}
	
	public List<Location> getPathToFirstBusStop()
	{
		return mPathToStart;
	}
	
	public List<Location> getPathToDestination()
	{
		return mPathFromFinalList;
	}
}
