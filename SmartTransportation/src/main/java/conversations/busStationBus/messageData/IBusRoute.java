package conversations.busStationBus.messageData;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public interface IBusRoute 
{
	public List<Location> getBusStopsLocations();
	public List<Location> getPathToTravel();
	public UUID getBusRouteID();
}
