package conversations.userMediator.messages;

import transportOffers.ITransportOffer;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class ConfirmTransportOfferMessage extends UnicastMessage<ITransportOffer>
{
	public ConfirmTransportOfferMessage(ITransportOffer offer, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		assert(offer != null);
		data = offer;		
	}
}
