package conversations.userMediator.messages.messageData;

import java.util.UUID;

import agents.User.TransportPreference;
import agents.User.TransportSortingPreference;


import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public class TransportServiceRequest implements TimeDriven, ITransportServiceRequest
{
	private Location mStartLocation;
	private Location mDestination;
	private TransportPreference mTransportPreference;
	private TransportSortingPreference mTransportSortingPreference;
	private String mMessage;
	private UUID mUserID;
	private UUID mAuthKey;
	private NetworkAddress mUserAddress;
	private int mTargetTravelTime;
	private int mTimeOutTimeSteps;
	private int mAgeTimeSteps;
	private boolean mIsValid;
	
	public TransportServiceRequest(Location startLocation, Location destination, 
			int targetTravelTime,
			TransportPreference transportPreference,
			TransportSortingPreference transportSortingPreference,
			UUID userID, UUID authKey, NetworkAddress userAddress) 
	{
		this(startLocation, destination, targetTravelTime, transportPreference, transportSortingPreference, 
				userID, authKey, userAddress, -1);
	}
	
	public TransportServiceRequest(Location startLocation, Location destination, 
			int targetTravelTime, 
			TransportPreference transportPreference, 
			TransportSortingPreference transportSortingPreference,
			UUID userID, UUID authKey, NetworkAddress userAddress, int timeOutTimeSteps)
	{
		assert(startLocation != null);
		assert(destination != null);
		assert(transportPreference != null);
		assert(transportSortingPreference != null);
		assert(userID != null);
		assert(authKey != null);
		assert(userAddress != null);
		assert(targetTravelTime > 0);
		
		mStartLocation = startLocation;
		mDestination = destination;
		mTargetTravelTime = targetTravelTime;
		mTransportPreference = transportPreference;
		mTransportSortingPreference = transportSortingPreference;
		mUserID = userID;
		mAuthKey = authKey;
		mUserAddress = userAddress;
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
		++mAgeTimeSteps;
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
	public int getTargetTravelTime() 
	{
		return mTargetTravelTime;
	}
	
	@Override
	public int getRequestAge()
	{
		return mAgeTimeSteps;
	}
	
	@Override
	public TransportPreference getTransportPreference() 
	{
		return mTransportPreference;
	}
	
	@Override
	public TransportSortingPreference getTransportSortingPreference() 
	{
		return mTransportSortingPreference;
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
	public NetworkAddress getUserNetworkAddress() 
	{
		return mUserAddress;
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
