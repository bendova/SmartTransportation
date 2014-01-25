package conversations.busStationBus;

import java.util.UUID;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class RegisterAsBusMessage extends UnicastMessage<UUID>
{
	public RegisterAsBusMessage(UUID busID, NetworkAddress from, 
			NetworkAddress to)
	{
		super(Performative.PROPOSE, from, to, new TimeStamp());
		
		assert(busID != null);
		data = busID;
	}
}
