package messageData;

import uk.ac.imperial.presage2.util.location.Location;

public class TaxiServiceRequest extends UserRequest
{
	private int mTimeSteps;
	private int mStartTimeStep;
	
	public TaxiServiceRequest(Location location) 
	{
		this(location, -1);
	}
	
	public TaxiServiceRequest(Location location, int timeSteps)
	{
		super(location, "I need a taxi!");
		mTimeSteps = timeSteps;
		mStartTimeStep = 0 ; //TODO get the current time steps here, somehow
	}
	
	public boolean isValid()
	{
		if(mTimeSteps > 0)
		{
			// FIXME
			//return ((/*CurrentTimeStep()*/ - mStartTimeStep) > mTimeSteps);
			return true;
		}
		return true;
	}
}
