package conversations.userTaxi.messages;

import conversations.userTaxi.messages.messageData.IConfirmationRequest;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class RequestTaxiConfirmationMessage extends UnicastMessage<IConfirmationRequest>
{
	public RequestTaxiConfirmationMessage(IConfirmationRequest confirmationRequest, 
			NetworkAddress from, NetworkAddress to) 
	{
		super(Performative.QUERY_IF, from, to, new TimeStamp());
	
		assert(confirmationRequest != null);
		data = confirmationRequest;
	}
}
