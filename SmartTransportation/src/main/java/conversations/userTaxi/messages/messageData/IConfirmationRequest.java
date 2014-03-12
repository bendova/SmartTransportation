package conversations.userTaxi.messages.messageData;

import uk.ac.imperial.presage2.core.network.NetworkAddress;

public interface IConfirmationRequest 
{
	double getTotalTravelTime();
	double getTravelCost();
	NetworkAddress getUserAddress();
}
