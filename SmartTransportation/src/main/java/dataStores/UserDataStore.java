package dataStores;

import gui.agents.AgentDataForMap.AgentType;

import java.util.UUID;

import agents.User.TransportMode;
import agents.User.TransportPreference;

import uk.ac.imperial.presage2.util.location.Location;

public class UserDataStore extends AgentDataStore
{
	private double mTargetTravelTime;
	private double mActualTravelTime;
	private boolean mHasReachedDestination;
	private TransportPreference mTransportPreference;
	private TransportMode mTransportModeUsed;
	
	public UserDataStore(String agentName, UUID agentID, Location startLocation)
	{
		super(agentName, agentID, AgentType.USER, startLocation);
		
		mTargetTravelTime = 0;
		mActualTravelTime = 0;
		mHasReachedDestination = false;
		mTransportPreference = TransportPreference.NO_PREFERENCE;
		mTransportModeUsed = TransportMode.NONE;
	}
	public void setTargetTravelTime(double target)
	{
		mTargetTravelTime = target;
	}
	public double getTargetTravelTime()
	{
		return mTargetTravelTime;
	}
	public void setActualTravelTime(double travelTime)
	{
		mActualTravelTime = travelTime;
	}
	public double getActualTravelTime()
	{
		return mActualTravelTime;
	}
	public void setHasReachedDestination(boolean value)
	{
		mHasReachedDestination = value;
	}
	public boolean getHasReachedDestination()
	{
		return mHasReachedDestination;
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
