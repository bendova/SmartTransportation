package agents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	}
	
	private void processRequest(TaxiServiceRequestMessage taxiServiceRequestMessage)
	{
		mPendingTaxiServiceRequests.add(taxiServiceRequestMessage);
		if(mTaxiStations.isEmpty() == false)
		{
			for (NetworkAddress toTaxiStation : mTaxiStations) 
			{
				forwardRequest(taxiServiceRequestMessage, toTaxiStation);
			}
		}
	}
	
	private void processRequest(RegisterAsTaxiStationMessage registerMessage)
	{
		NetworkAddress taxiStation = registerMessage.getFrom();
		mTaxiStations.add(taxiStation);
		if(mPendingTaxiServiceRequests.isEmpty() == false)
		{
			for (TaxiServiceRequestMessage taxiServiceRequestMessage : mPendingTaxiServiceRequests) 
			{
				if(taxiServiceRequestMessage.getData().isValid())
				{
					forwardRequest(taxiServiceRequestMessage, taxiStation);
				}
				else 
				{
					mPendingTaxiServiceRequests.remove(taxiServiceRequestMessage);
				}
			}
		}
	}
	
	private void forwardRequest(TaxiServiceRequestMessage requestMessage, NetworkAddress toTaxiStation)
	{
		logger.info("ForwardRequest() requestMessage " + requestMessage);
		
		TaxiServiceRequestMessage forwardMessage = new TaxiServiceRequestMessage
				(requestMessage.getData(), requestMessage.getFrom(), toTaxiStation);
		network.sendMessage(forwardMessage);
	}
}
