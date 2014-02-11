package conversations.busStationBus.messageData;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class BusRoute implements IBusRoute
{
	private List<Location> mBusStops;
	private List<Location> mPathToTravel;
	private UUID mBusRouteID;
	
	public BusRoute(List<Location> busStops, List<Location> pathToTravel, UUID busRouteID)
	{
		assert(busStops != null);
		assert(pathToTravel != null);
		assert(busRouteID != null);
		
		mBusStops = busStops;
		mPathToTravel = pathToTravel;
		mBusRouteID = busRouteID;
	}

	@Override
	public List<Location> getBusStops() 
	{
		return mBusStops;
	}

	@Override
	public UUID getBusRouteID() 
	{
		return mBusRouteID;
	}

	@Override
	public List<Location> getPathToTravel() 
	{
		return mPathToTravel;
	}
}
