package conversations.userTaxi.messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiRequestCancelMessage extends UnicastMessage<String>
{
	public TaxiRequestCancelMessage(String message, 
			NetworkAddress from, NetworkAddress to) 
	{
		super(Performative.CANCEL, from, to, new TimeStamp());
	
		assert(message != null);
		data = message;
	}
}
