package messages;

import java.util.UUID;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiOrderCompleteMessage extends UnicastMessage<UUID>
{
	public TaxiOrderCompleteMessage(UUID taxiID, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.INFORM, from, to, new TimeStamp());
		
		assert(taxiID != null);
		data = taxiID;
	}
}
