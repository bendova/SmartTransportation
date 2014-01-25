package conversations.userBusStation;

import java.util.List;

import uk.ac.imperial.presage2.util.location.Location;

public interface IBusTravelPlan 
{
	public List<Location> getPathToFirstBusStop();
	public List<Location> getPathToDestination();
}
