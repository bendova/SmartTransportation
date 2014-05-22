package agents.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import agents.User.TransportPreference;

import conversations.userMediator.messages.TransportServiceOfferMessage;
import conversations.userMediator.messages.messageData.ITransportServiceOffer;
import conversations.userMediator.messages.messageData.ITransportServiceRequest;
import conversations.userMediator.messages.messageData.TransportServiceOffer;

import transportOffers.ITransportOffer;
import transportOffers.TransportOffer;
import uk.ac.imperial.presage2.core.network.NetworkAdaptor;
import uk.ac.imperial.presage2.core.network.NetworkAddress;

public class TransportServiceRecord implements ITransportServiceRecord
{
	private NetworkAddress mUserAddress;
	private NetworkAddress mMediatorAddress;
	private NetworkAdaptor mNetworkAdaptor;
	private Logger mLogger;
	private ITransportServiceRequest mTransportServiceRequest;
	private List<TransportOffer> mTransportOffers;
	private Comparator<TransportOffer> mTransportOffersComparator;
	private boolean mHasBeenUpdated;
	private boolean mCanShareTransportOffers;
	
	public TransportServiceRecord(NetworkAddress userAddress, ITransportServiceRequest transportServiceRequest,
			NetworkAddress mediatorAddress, NetworkAdaptor adaptor, Logger logger, 
			Comparator<TransportOffer> comparator)
	{
		assert(userAddress != null);
		assert(transportServiceRequest != null);
		assert(mediatorAddress != null);
		assert(adaptor != null);
		assert(logger != null);
		assert(comparator != null);
		
		mUserAddress = userAddress;
		mTransportServiceRequest = transportServiceRequest;
		mMediatorAddress = mediatorAddress;
		mNetworkAdaptor = adaptor;
		mLogger = logger;
		mTransportOffersComparator = comparator;
		mTransportOffers = new ArrayList<TransportOffer>();
		mHasBeenUpdated = false;
		mCanShareTransportOffers = true;
	}
	
	@Override
	public List<TransportOffer> getTransportOffers() 
	{
		return Collections.unmodifiableList(mTransportOffers);
	}

	@Override
	public void addTransportOffer(TransportOffer offer) 
	{
		assert(offer != null);
		assert(mTransportOffers.contains(offer) == false);
		
		mLogger.info("addTransportOffer() offer " + offer);
		mLogger.info("addTransportOffer() mUserAddress " + mUserAddress);
		
		mTransportOffers.add(offer);
		mHasBeenUpdated = true;
	}
	
	@Override
	public void removeTransportOffer(TransportOffer offer) 
	{
		assert(offer != null);
		assert(mTransportOffers.contains(offer) == true);
		
		if(mCanShareTransportOffers)
		{
			mLogger.info("removeTransportOffer() offer " + offer);
			mLogger.info("removeTransportOffer() mUserAddress " + mUserAddress);
			
			mTransportOffers.remove(offer);
			mHasBeenUpdated = true;
		}
	}
	
	@Override
	public void sendTransportOffers() 
	{
		if(mHasBeenUpdated && (mTransportOffers.isEmpty() == false))
		{
			mLogger.info("sendTransportOffers() mUserAddress " + mUserAddress);
			
			ITransportServiceOffer offer = new TransportServiceOffer(new ArrayList<ITransportOffer>(mTransportOffers));
			TransportServiceOfferMessage msg = new TransportServiceOfferMessage(offer, mMediatorAddress, 
					mUserAddress);
			mNetworkAdaptor.sendMessage(msg);
			
			mHasBeenUpdated = false;
			mCanShareTransportOffers = false;
		}
	}
	@Override
	public ITransportServiceRequest getTransportServiceRequest() 
	{
		return mTransportServiceRequest;
	}
		
	@Override
	public void sortTransportOffers()
	{
		if(mHasBeenUpdated)
		{
			TransportPreference preference = mTransportServiceRequest.getTransportPreference();
			for (Iterator<TransportOffer> iterator = mTransportOffers.iterator(); iterator.hasNext();) 
			{
				iterator.next().applyTransportPreference(preference);
			}
			
			if(mTransportOffers.size() > 1)
			{
				Collections.sort(mTransportOffers, mTransportOffersComparator);
			}
		}
	}

	@Override
	public boolean canShareTransportOffers()
	{
		return mCanShareTransportOffers;
	}
}
