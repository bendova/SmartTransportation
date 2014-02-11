package conversations.userBusStation;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class BusTravelPlan implements IBusTravelPlan
{
	private List<Location> mPathToStart;
	private List<Location> mPathFromFinalList;
	private int mBusTravelDistance;
	private UUID mBusRouteID;
	
	public BusTravelPlan(List<Location> pathToStartBusStop,
						 List<Location> pathFromFinalBusStop,
						 int busTravelDistance,
						 UUID busRouteID)
	{
		assert(pathToStartBusStop != null);
		assert(pathFromFinalBusStop != null);
		assert(busTravelDistance > 0);
		assert(busRouteID != null);
		
		mPathToStart = pathToStartBusStop;
		mPathFromFinalList = pathFromFinalBusStop;
		mBusTravelDistance = busTravelDistance;
		mBusRouteID = busRouteID;
	}

	@Override
	public List<Location> getPathToFirstBusStop()
	{
		return mPathToStart;
	}
	
	@Override
	public List<Location> getPathToDestination()
	{
		return mPathFromFinalList;
	}
	
	@Override
	public int getBusTravelDistance()
	{
		return mBusTravelDistance;
	}
	
	@Override
	public UUID getBusRouteID() 
	{
		return mBusRouteID;
	}
}
