package conversations.userMediator.messages.messageData;

import java.util.List;

import transportOffers.ITransportOffer;
import transportOffers.TransportOffer;

public class TransportServiceOffer implements ITransportServiceOffer
{
	private List<ITransportOffer> mTransportOffers;
	public TransportServiceOffer(List<ITransportOffer> transportOffers)
	{
		assert(transportOffers != null);
		assert(transportOffers.size() > 0);
		
		mTransportOffers = transportOffers;
	}
	
	@Override
	public List<ITransportOffer> getTransportOffers() 
	{
		return mTransportOffers;
	}
}
