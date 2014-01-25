package conversations.userBus.messages.messageData;

import conversations.busStationBus.messageData.IBusRoute;
import uk.ac.imperial.presage2.util.location.Location;

public class BusStopArrivalNotification implements IBusStopArrivalNotification
{
	private Location mBusStopLocation;
	private IBusRoute mBusRoute;
	
	public BusStopArrivalNotification(Location busStopLocation, IBusRoute busRoute)
	{
		assert(busStopLocation != null);
		assert(busRoute != null);
		
		mBusStopLocation = busStopLocation;
		mBusRoute = busRoute;
	}

	@Override
	public Location getBusStopLocation() 
	{
		return mBusStopLocation;
	}

	@Override
	public IBusRoute getBusRoute() 
	{
		return mBusRoute;
	}
}
