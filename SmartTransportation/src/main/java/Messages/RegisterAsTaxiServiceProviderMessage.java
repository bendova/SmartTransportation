package Messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;

public class RegisterAsTaxiServiceProviderMessage extends UnicastMessage<String>
{
	public RegisterAsTaxiServiceProviderMessage(NetworkAddress from, NetworkAddress to)
	{
		super(Performative.ACCEPT_PROPOSAL, from, to, new TimeStamp());
		data = "I want to register as a Taxi Service Provider!";
	}
}
