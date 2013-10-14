package Agents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import org.drools.process.core.datatype.impl.NewInstanceDataTypeFactory;
import org.drools.rule.Function;
import org.omg.PortableInterceptor.ForwardRequest;

import Messages.*;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.Message;
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
			else if (input instanceof RegisterAsTaxiServiceProviderMessage)
			{
				processRequest((RegisterAsTaxiServiceProviderMessage)input);
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
	
	private void processRequest(RegisterAsTaxiServiceProviderMessage taxiStationNetworkAddress)
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
