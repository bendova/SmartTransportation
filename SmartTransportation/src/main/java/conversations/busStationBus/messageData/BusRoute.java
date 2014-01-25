package conversations.busStationBus.messageData;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class BusRoute implements IBusRoute
{
	private List<Location> mBusStops;
	private UUID mBusRouteID;
	
	public BusRoute(List<Location> busStop, UUID busRouteID)
	{
		assert(busStop != null);
		assert(busRouteID != null);
		
		mBusStops = busStop;
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
}
