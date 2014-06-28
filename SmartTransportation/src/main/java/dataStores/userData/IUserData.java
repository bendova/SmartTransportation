package dataStores.userData;

import java.util.List;

import dataStores.agentData.IAgentData;
import agents.User.TransportMode;
import agents.User.TransportPreference;

public interface IUserData extends IAgentData
{
	double getTargetTravelTime();
	double getActualTravelTime();
	boolean getHasReachedDestination();
	boolean getHasReachedDestinationOnTime();
	TransportPreference getTransportPreference();
	TransportMode getTransportMethodUsed();
	List<UserEvent> getUserEvents();
}
