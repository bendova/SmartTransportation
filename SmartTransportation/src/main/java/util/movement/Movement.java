package util.movement;

import uk.ac.imperial.presage2.util.location.Location;

public class Movement 
{
	private Location mLocation;
	private int mTimeTakenPerUnitDistance;
	private int mStartTime;
	public Movement(Location location, int timeTakenPerUnitDistance,
			int startTime)
	{
		assert(location != null);
		assert(timeTakenPerUnitDistance > 0);
		assert(startTime >= 0);
		
		mLocation = location;
		mTimeTakenPerUnitDistance = timeTakenPerUnitDistance;
		mStartTime = startTime;
	}
	public Location getLocation()
	{
		return mLocation;
	}
	public int getTimeTakenPerUnitDistance()
	{
		return mTimeTakenPerUnitDistance;
	}
	public int getStartTime()
	{
		return mStartTime;
	}
}
