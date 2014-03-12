package conversations.busStationBus.messageData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class BusRoute implements IBusRoute
{
	private List<Location> mBusStops;
	private Map<Location, List<Location>> mPathsBetweenBusStops;
	private UUID mBusRouteID;
	
	public BusRoute(UUID busRouteID, List<Location> busStops, 
			Map<Location, List<Location>> pathsBetweenBusStops)
	{
		assert(busRouteID != null);
		assert(busStops != null);
		assert(pathsBetweenBusStops != null);
		
		mBusStops = busStops;
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
	public Map<Location, List<Location>> getPathsBetweenBusStops()
	{
		return mPathsBetweenBusStops;
	}
}
