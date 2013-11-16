package agents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import messages.*;
import messages.messageData.UserRequest;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

// TODO find a better a name for this class
public class Mediator extends AbstractParticipant
{
	HashSet<NetworkAddress> mTaxiStations;
	HashMap<UserRequest, User> mUserRequestsMap;
	Queue<TaxiServiceRequestMessage> mPendingTaxiServiceRequests;
	
	public Mediator(UUID id, String name) 
	{
		super(id, name);
		
		mTaxiStations = new HashSet<NetworkAddress>();
		mUserRequestsMap = new HashMap<UserRequest, User>();
		mPendingTaxiServiceRequests = new PriorityBlockingQueue<TaxiServiceRequestMessage>();
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null)
		{
			if(input instanceof TaxiServiceRequestMessage)
			{
				processRequest((TaxiServiceRequestMessage)input);
			}
			else if (input instanceof RegisterAsTaxiStationMessage)
			{
				processRequest((RegisterAsTaxiStationMessage)input);
			}
		}
	}
	
	public NetworkAddress getNetworkAddress() 
	{
		return network.getAddress();
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
//		logger.info("incrementTime() " + getTime());
		
		if((mPendingTaxiServiceRequests.isEmpty() == false) && (mTaxiStations.isEmpty() == false))
		{
			for (TaxiServiceRequestMessage taxiServiceRequestMessage : mPendingTaxiServiceRequests) 
			{
				ForwardRequest(taxiServiceRequestMessage);
				mPendingTaxiServiceRequests.remove(taxiServiceRequestMessage);
			}
		}
	}
	
	private void processRequest(TaxiServiceRequestMessage taxiServiceRequestMessage)
	{
		if(mTaxiStations.isEmpty())
		{
			mPendingTaxiServiceRequests.add(taxiServiceRequestMessage);
		}
		else
		{
			ForwardRequest(taxiServiceRequestMessage);
		}
	}
	
	private void processRequest(RegisterAsTaxiStationMessage taxiStationNetworkAddress)
	{
		mTaxiStations.add(taxiStationNetworkAddress.getFrom());
	}
	
	private void ForwardRequest(TaxiServiceRequestMessage requestMessage)
	{
		logger.info("ForwardRequest() requestMessage " + requestMessage);
		
		TaxiServiceRequestMessage forwardMessage;
		for (NetworkAddress toTaxiStation : mTaxiStations) 
		{
			forwardMessage = new TaxiServiceRequestMessage(requestMessage.getData(), requestMessage.getFrom(), toTaxiStation);
			network.sendMessage(forwardMessage);
		}
	}
}
