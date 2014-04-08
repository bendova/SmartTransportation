package conversations.taxiStationMediator;

import conversations.taxiStationMediator.messageData.ITaxiDescription;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiAvailableMessage extends UnicastMessage<ITaxiDescription>
{
	public TaxiAvailableMessage(ITaxiDescription taxiDescription,
			NetworkAddress from, NetworkAddress to)
	{
		super(Performative.INFORM, from, to, new TimeStamp());
		data = taxiDescription;
	}
}
