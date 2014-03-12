package conversations.userMediator.messages;

import transportOffers.TransportOffer;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class ConfirmTransportOfferMessage extends UnicastMessage<TransportOffer>
{
	public ConfirmTransportOfferMessage(TransportOffer offer, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		assert(offer != null);
		data = offer;		
	}
}
