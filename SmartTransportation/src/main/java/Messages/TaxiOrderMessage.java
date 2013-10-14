package Messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;

public class TaxiOrderMessage extends UnicastMessage<TaxiOrder>
{
	public TaxiOrderMessage(TaxiOrder taxiOrder, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		assert(taxiOrder != null);
		data = taxiOrder;
	}
}
