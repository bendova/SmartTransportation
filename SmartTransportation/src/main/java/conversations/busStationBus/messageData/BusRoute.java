package conversations.busStationBus.messageData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class BusRoute implements IBusRoute
{
	private List<Location> mBusStops;
	private List<Location> mPathToTravel;
	private Map<Location, List<Location>> mPathsBetweenBusStops;
	private UUID mBusRouteID;
	
	public BusRoute(UUID busRouteID, List<Location> busStops, List<Location> pathToTravel, 
			Map<Location, List<Location>> pathsBetweenBusStops)
	{
		assert(busStops != null);
		assert(pathToTravel != null);
		assert(busRouteID != null);
		
		mBusStops = busStops;
		mPathToTravel = pathToTravel;
		mPathsBetweenBusStops = pathsBetweenBusStops;
		mBusRouteID = busRouteID;
	}

	@Override
	public List<Location> getBusStopsLocations() 
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
	
	public Map<Location, List<Location>> getPathsBetweenBusStops()
	{
		return mPathsBetweenBusStops;
	}
}
