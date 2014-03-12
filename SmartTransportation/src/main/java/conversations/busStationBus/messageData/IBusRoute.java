package conversations.busStationBus.messageData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public interface IBusRoute 
{
	public List<Location> getBusStopsLocations();
	public Map<Location, List<Location>> getPathsBetweenBusStops();
	public UUID getBusRouteID();
}
