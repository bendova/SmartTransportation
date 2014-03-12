package transportOffers;

import uk.ac.imperial.presage2.core.network.NetworkAdaptor;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import agents.User.TransportMode;
import conversations.userTaxi.messages.RequestTaxiConfirmationMessage;
import conversations.userTaxi.messages.TaxiRequestCancelMessage;
import conversations.userTaxi.messages.TaxiRequestConfirmationMessage;

public class TaxiTransportOffer extends TransportOffer
{
	private NetworkAddress mTaxiStationAddress;
	private NetworkAddress mRequestorAddress;
	private NetworkAdaptor mNetworkAdaptor;
	public TaxiTransportOffer(NetworkAdaptor networkAdaptor,
			NetworkAddress requestorAddress)
	{
		super(TransportMode.TAKE_TAXI);
		
		assert(networkAdaptor != null);
		assert(requestorAddress != null);
		
		mTravelCost = taxiOffer.getData().getTravelCost();
		mTravelTime = taxiOffer.getData().getTotalTravelTime();
		mTaxiStationAddress = taxiOffer.getFrom();
		mRequestorAddress = requestorAddress;
		mNetworkAdaptor = networkAdaptor;
	}

	@Override
	public void confirm() 
	{
		sendConfirmationMessage();
	}

	@Override
	public void cancel() 
	{
		sendCancelMessage();
	}
	
	private void sendConfirmationMessage()
	{
		TaxiRequestConfirmationMessage confirmationMessage = new TaxiRequestConfirmationMessage("I confirm the request",
				mRequestorAddress, mTaxiStationAddress);
		mNetworkAdaptor.sendMessage(confirmationMessage);
	}
	private void sendCancelMessage()
	{
		TaxiRequestCancelMessage cancelMessage = new TaxiRequestCancelMessage("I cancel this request", 
				mRequestorAddress, mTaxiStationAddress);
		mNetworkAdaptor.sendMessage(cancelMessage);
	}
}
