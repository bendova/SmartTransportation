package dataStores;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javafx.beans.property.StringProperty;

import agents.User.TransportMode;
import agents.User.TransportPreference;

import uk.ac.imperial.presage2.util.location.Location;

public class UserDataStore 
{
	private String mUserName;
	private UUID mUserID;
	private double mTravelTimeTarget;
	private double mTravelTime;
	private boolean mHasReachedDestination;
	private List<Location> mUserPath;
	private TransportPreference mTransportPreference;
	private TransportMode mTransportModeUsed;
	
	public UserDataStore(String userName, UUID userID)
	{
		assert(userName != null);
		assert(userID != null);
		
		mUserName = userName;
		mUserID = userID;
		
		mTravelTimeTarget = 0;
		mTravelTime = 0;
		mHasReachedDestination = false;
		mUserPath = new ArrayList<Location>();
		mTransportPreference = TransportPreference.NO_PREFERENCE;
		mTransportModeUsed = TransportMode.NONE;
	}
	public String getName()
	{
		return mUserName;
	}
	public UUID getID()
	{
		return mUserID;
	}
	public void setTravelTimeTarget(double target)
	{
		mTravelTimeTarget = target;
	}
	public double getTravelTimeTarget()
	{
		return mTravelTimeTarget;
	}
	public void setTravelTime(double travelTime)
	{
		mTravelTime = travelTime;
	}
	public double getTravelTime()
	{
		return mTravelTime;
	}
	public void setHasReachedDestination(boolean value)
	{
		mHasReachedDestination = value;
	}
	public boolean getHasReachedDestination()
	{
		return mHasReachedDestination;
	}
	public void setPathTraveled(List<Location> path)
	{
		mUserPath = path;
	}
	public List<Location> getPathTraveled()
	{
		return mUserPath;
	}
	public void setTransportPreference(TransportPreference pref)
	{
		mTransportPreference = pref;
	}
	public TransportPreference getTransportPreference()
	{
		return mTransportPreference;
	}
	public void setTransportMethodUsed(TransportMode transportModeUsed)
	{
		mTransportModeUsed = transportModeUsed;
	}
	public TransportMode getTransportMethodUsed()
	{
		return mTransportModeUsed;
	}
}
