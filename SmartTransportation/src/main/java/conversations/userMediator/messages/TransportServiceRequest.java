package conversations.userMediator.messages;

import java.util.UUID;


import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.util.location.Location;

public class TransportServiceRequest implements TimeDriven, ITransportServiceRequest
{
	private Location mStartLocation;
	private Location mDestination;
	private String mMessage;
	private UUID mUserID;
	private UUID mAuthKey;
	private int mTimeOutTimeSteps;
	private int mAgeTimeSteps;
	private boolean mIsValid;
	
	public TransportServiceRequest(Location startLocation, Location destination, 
			UUID userID, UUID authKey) 
	{
		this(startLocation, destination, userID, authKey, -1);
	}
	
	public TransportServiceRequest(Location startLocation, Location destination, 
			UUID userID, UUID authKey, int timeOutTimeSteps)
	{
		assert(startLocation != null);
		assert(destination != null);
		assert(userID != null);
		
		mStartLocation = startLocation;
		mDestination = destination;
		mUserID = userID;
		mAuthKey = authKey;
		mMessage = "I need to get to " + mDestination;
		mTimeOutTimeSteps = timeOutTimeSteps;
		mAgeTimeSteps = 0;
		mIsValid = true;
	}
	
	@Override
	public boolean isValid()
	{
		if(mIsValid && (mTimeOutTimeSteps > 0))
		{
			mIsValid = (mAgeTimeSteps > mTimeOutTimeSteps);
		}
		return mIsValid;
	}
	
	public void cancel()
	{
		mIsValid = false;
	}
	
	@Override
	public void incrementTime() 
	{
		mAgeTimeSteps++;
	}
	
	@Override
	public Location getStartLocation() 
	{
		return mStartLocation;
	}
	
	@Override
	public Location getDestination()
	{
		return mDestination;
	}
	
	@Override
	public String getMessage() 
	{
		return mMessage;
	}
	
	@Override
	public UUID getUserID()
	{
		return mUserID;
	}
	
	@Override
	public UUID getUserAuthKey()
	{
		return mAuthKey;
	}
	
	@Override
	public int hashCode()
	{
		return this.getUserID().hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if((obj != null) && (obj instanceof TransportServiceRequest))
		{
			return (this.getUserID() == ((TransportServiceRequest)obj).getUserID());
		}
		return false;
	}
}
