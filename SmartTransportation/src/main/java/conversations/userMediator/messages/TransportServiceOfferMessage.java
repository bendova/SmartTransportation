package conversations.userMediator.messages;

import conversations.userMediator.messages.messageData.ITransportServiceOffer;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TransportServiceOfferMessage extends UnicastMessage<ITransportServiceOffer>
{
	public TransportServiceOfferMessage(ITransportServiceOffer offer, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		assert(offer != null);
		data = offer;		
	}
}
