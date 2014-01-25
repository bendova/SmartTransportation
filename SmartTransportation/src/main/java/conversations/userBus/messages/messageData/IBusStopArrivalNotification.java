package conversations.userBus.messages.messageData;

import conversations.busStationBus.messageData.IBusRoute;

import uk.ac.imperial.presage2.util.location.Location;

public interface IBusStopArrivalNotification 
{
	public Location getBusStopLocation();
	public IBusRoute getBusRoute();
}
