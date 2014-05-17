package conversations.userBusStation;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public class BusTravelPlan implements IBusTravelPlan
{
	private List<Location> mPathToStart;
	private Location mStartBusStop;
	private List<Location> mPathFromFinalList;
	private Location mFinalBusStop;
	private int mBusTravelDistance;
	private UUID mBusRouteID;
	private NetworkAddress mUserAddress;
	
	public BusTravelPlan(List<Location> pathToStartBusStop,
						 Location startBusStop,
						 List<Location> pathFromFinalBusStop,
						 Location finalBusStop,
						 int busTravelDistance,
						 UUID busRouteID,
						 NetworkAddress userAddress)
	{
		assert(pathToStartBusStop != null);
		assert(startBusStop != null);
		assert(pathFromFinalBusStop != null);
		assert(finalBusStop != null);
		assert(busTravelDistance > 0);
		assert(busRouteID != null);
		assert(userAddress != null);
		
		mPathToStart = pathToStartBusStop;
		mStartBusStop = startBusStop;
		mPathFromFinalList = pathFromFinalBusStop;
		mFinalBusStop = finalBusStop;
		mBusTravelDistance = busTravelDistance;
		mBusRouteID = busRouteID;
		mUserAddress = userAddress;
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

	@Override
	public NetworkAddress getUserAddress() 
	{
		return mUserAddress;
	}

	@Override
	public Location getFirstBusStopLocation() 
	{
		return mStartBusStop;
	}

	@Override
	public Location getDestinationBusStopLocation() 
	{
		return mFinalBusStop;
	}
}
