package conversations.taxiStationMediator.messageData;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public interface ITaxiDescription 
{
	NetworkAddress getTaxiAddress();
	NetworkAddress getTaxiStationAddress();
	Location getTaxiLocation();
}
