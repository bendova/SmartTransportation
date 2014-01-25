package conversations.taxiStationTaxi.messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;
import agents.Taxi.Status;

public class TaxiStatusUpdateMessage extends UnicastMessage<Status>
{
	public TaxiStatusUpdateMessage(Status currentStatus, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		assert(currentStatus != null);
		data = currentStatus;		
	}
}
