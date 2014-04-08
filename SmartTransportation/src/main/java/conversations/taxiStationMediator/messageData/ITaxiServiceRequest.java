package conversations.taxiStationMediator.messageData;

import java.util.UUID;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public interface ITaxiServiceRequest 
{
	UUID getUserID();
	UUID getUserAuthKey();
	NetworkAddress getUserNetworkAddress();
	Location getStartLocation();
	ITaxiDescription getTaxiDescription();
}
