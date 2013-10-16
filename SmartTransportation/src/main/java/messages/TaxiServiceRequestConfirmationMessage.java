package messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiServiceRequestConfirmationMessage extends UnicastMessage<String>
{
	public TaxiServiceRequestConfirmationMessage(String message, 
			NetworkAddress from, NetworkAddress to) 
	{
		super(Performative.CONFIRM, from, to, new TimeStamp());
	
		assert(message != null);
		data = message;
	}
}
