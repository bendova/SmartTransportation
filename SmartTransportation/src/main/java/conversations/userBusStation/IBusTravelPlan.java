package conversations.userBusStation;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public interface IBusTravelPlan 
{
	public NetworkAddress getUserAddress();
	public List<Location> getPathToFirstBusStop();
	public int getBusTravelDistance();
	public List<Location> getPathToDestination();
	public UUID getBusRouteID();
}
