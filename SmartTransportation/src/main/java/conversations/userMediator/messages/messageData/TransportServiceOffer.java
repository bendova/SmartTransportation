package conversations.userMediator.messages.messageData;

import java.util.List;

import transportOffers.TransportOffer;

public class TransportServiceOffer implements ITransportServiceOffer
{
	private List<TransportOffer> mTransportOffers;
	public TransportServiceOffer(List<TransportOffer> transportOffers)
	{
		assert(transportOffers != null);
		assert(transportOffers.size() > 0);
		
		mTransportOffers = transportOffers;
	}
	
	@Override
	public List<TransportOffer> getTransportOffers() 
	{
		return mTransportOffers;
	}
}
