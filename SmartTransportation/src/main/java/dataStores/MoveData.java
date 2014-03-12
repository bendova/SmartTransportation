package dataStores;

import uk.ac.imperial.presage2.util.location.Location;

public class MoveData 
{
	private Location mLocation;
	private int mTimeTakenPerUnitDistance;
	public MoveData(Location location, int timeTakenPerUnitDistance)
	{
		assert(location != null);
		assert(timeTakenPerUnitDistance >= 0);
		
		mLocation = location;
		mTimeTakenPerUnitDistance = timeTakenPerUnitDistance;
	}
	public Location getLocation()
	{
		return mLocation;
	}
	public int getTimeTakenPerUnitDistance()
	{
		return mTimeTakenPerUnitDistance;
	}
}
