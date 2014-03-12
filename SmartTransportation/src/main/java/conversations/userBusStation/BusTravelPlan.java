package conversations.userBusStation;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public class BusTravelPlan implements IBusTravelPlan
{
	private List<Location> mPathToStart;
	private List<Location> mPathFromFinalList;
	private int mBusTravelDistance;
	private UUID mBusRouteID;
	private NetworkAddress mUserAddress;
	
	public BusTravelPlan(List<Location> pathToStartBusStop,
						 List<Location> pathFromFinalBusStop,
						 int busTravelDistance,
						 UUID busRouteID,
						 NetworkAddress userAddress)
	{
		assert(pathToStartBusStop != null);
		assert(pathFromFinalBusStop != null);
		assert(busTravelDistance > 0);
		assert(busRouteID != null);
		assert(userAddress != null);
		
		mPathToStart = pathToStartBusStop;
		mPathFromFinalList = pathFromFinalBusStop;
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
}
