package dataStores.userData;

import gui.agents.AgentDataForMap.AgentType;

import java.util.List;
import java.util.UUID;

import dataStores.agentData.AgentData;
import agents.User.TransportMode;
import agents.User.TransportPreference;
import uk.ac.imperial.presage2.util.location.Location;

public class UserData extends AgentData implements IUserData
{
	private double mTargetTravelTime;
	private double mActualTravelTime;
	private boolean mHasReachedDestination;
	private boolean mHasReachedDestinationOnTime;
	private TransportPreference mTransportPreference;
	private TransportMode mTransportModeUsed;
	private List<UserEvent> mEvents;
	
	public UserData(String agentName, UUID agentID, Location startLocation, List<UserEvent> events)
	{
		super(agentName, agentID, AgentType.USER, startLocation);
		
		assert(events != null);
		
		mTargetTravelTime = 0;
		mActualTravelTime = 0;
		mHasReachedDestination = false;
		mHasReachedDestinationOnTime = false;
		mTransportPreference = TransportPreference.NO_PREFERENCE;
		mTransportModeUsed = TransportMode.NONE;
		mEvents = events;
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
	public void setHasReachedDestinationOnTime(boolean value)
	{
		mHasReachedDestinationOnTime = value;
	}
	public boolean getHasReachedDestinationOnTime()
	{
		return mHasReachedDestinationOnTime;
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
	public List<UserEvent> getUserEvents()
	{
		return mEvents;
	}
}
