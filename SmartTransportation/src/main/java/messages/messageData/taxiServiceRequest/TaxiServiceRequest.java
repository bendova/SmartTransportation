package messages.messageData.taxiServiceRequest;

import java.util.UUID;

import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.util.location.Location;

public class TaxiServiceRequest implements TimeDriven, TaxiServiceRequestInterface
{
	private Location mLocation;
	private String mMessage;
	private UUID mUserID;
	private UUID mAuthKey;
	private int mTimeOutTimeSteps;
	private int mAgeTimeSteps;
	private boolean mIsValid;
	
	public TaxiServiceRequest(Location location, UUID userID, UUID authKey) 
	{
		this(location, userID, authKey, -1);
	}
	
	public TaxiServiceRequest(Location location, UUID userID, UUID authKey, int timeOutTimeSteps)
	{
		assert(location != null);
		assert(userID != null);
		
		mLocation = location;
		mUserID = userID;
		mAuthKey = authKey;
		mMessage = "I need a taxi!";
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
	public Location getLocation() 
	{
		return mLocation;
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
		if((obj != null) && (obj instanceof TaxiServiceRequest))
		{
			return (this.getUserID() == ((TaxiServiceRequest)obj).getUserID());
		}
		return false;
	}
}
