package conversations.userMediator.messages.messageData;

import java.util.UUID;

import conversations.taxiStationMediator.messageData.ITaxiServiceRequest;

import agents.User.TransportPreference;
import agents.User.TransportSortingPreference;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public interface ITransportServiceRequest
{
	Location getStartLocation();
	Location getDestination();
	int getTargetTravelTime();
	int getRequestAge();
	String getMessage();
	UUID getUserID();
	UUID getUserAuthKey();
	NetworkAddress getUserNetworkAddress();
	boolean isValid();
	
	//FIME I would prefer to have the transport preference
	// be visible only for Mediator.
	TransportPreference getTransportPreference();
	TransportSortingPreference getTransportSortingPreference();
}
