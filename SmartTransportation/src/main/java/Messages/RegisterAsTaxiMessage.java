package Messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;

public class RegisterAsTaxiMessage extends UnicastMessage<TaxiData>
{
	public RegisterAsTaxiMessage(TaxiData taxiData, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.ACCEPT_PROPOSAL, from, to, new TimeStamp());
		
		assert(taxiData != null);
		data = taxiData;
	}
}
