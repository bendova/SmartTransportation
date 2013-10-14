package messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class RegisterAsTaxiStationMessage extends UnicastMessage<String>
{
	public RegisterAsTaxiStationMessage(NetworkAddress from, NetworkAddress to)
	{
		super(Performative.ACCEPT_PROPOSAL, from, to, new TimeStamp());
		data = "I want to register as a Taxi Service Provider!";
	}
}
