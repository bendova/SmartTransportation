package util.movement;

import uk.ac.imperial.presage2.core.Action;
import uk.ac.imperial.presage2.util.location.Location;

public class TransportMove implements Action
{
	public static final int DEFAULT_PER_UNIT_DISTANCE = 1;
	
	private Location mTargetLocation;
	private int mTimePerUnitDistance;
	public TransportMove(Location target) 
	{
		this(target, DEFAULT_PER_UNIT_DISTANCE);
	}
	public TransportMove(Location target, int timePerUnitDistance) 
	{
		assert(target != null);
		assert(timePerUnitDistance >= 0);
		
		mTimePerUnitDistance = timePerUnitDistance;
		mTargetLocation = target;
	}
	public int getTimePerUnitDistance()
	{
		return mTimePerUnitDistance;
	}
	public Location getTargetLocation()
	{
		return mTargetLocation;
	}
}
