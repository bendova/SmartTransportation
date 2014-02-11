package agents;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import conversations.busStationMediator.RegisterAsBusServiceMessage;
import conversations.taxiStationMediator.RegisterAsTaxiStationMessage;
import conversations.userMediator.messages.TransportServiceRequestMessage;

import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

// TODO find a better a name for this class
public class Mediator extends AbstractParticipant
{
	private Set<NetworkAddress> mTaxiStations;
	private Set<NetworkAddress> mBusServices;
	private Queue<TransportServiceRequestMessage> mPendingServiceRequests;
	private Location mLocation = new Location(0, 0, 0);
	
	public Mediator(UUID id, String name) 
	{
		super(id, name);
		
		mTaxiStations = new HashSet<NetworkAddress>();
		mBusServices = new HashSet<NetworkAddress>();
		mPendingServiceRequests = new ConcurrentLinkedQueue<TransportServiceRequestMessage>();
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return ss;
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null)
		{
			if(input instanceof TransportServiceRequestMessage)
			{
				processRequest((TransportServiceRequestMessage)input);
			}
			else if (input instanceof RegisterAsTaxiStationMessage)
			{
				processRequest((RegisterAsTaxiStationMessage)input);
			}
			else if (input instanceof RegisterAsBusServiceMessage)
			{
				processRequest((RegisterAsBusServiceMessage)input);
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
	
	private void processRequest(TransportServiceRequestMessage serviceRequestMessage)
	{
		mPendingServiceRequests.add(serviceRequestMessage);
		if(mTaxiStations.isEmpty() == false)
		{
			for (NetworkAddress toTaxiStation : mTaxiStations) 
			{
				forwardRequest(serviceRequestMessage, toTaxiStation);
			}
		}
		if(mBusServices.isEmpty() == false)
		{
			for (NetworkAddress toBusStation : mBusServices) 
			{
				forwardRequest(serviceRequestMessage, toBusStation);
			}
		}
	}
	
	private void processRequest(RegisterAsTaxiStationMessage registerMessage)
	{
		NetworkAddress taxiStation = registerMessage.getFrom();
		mTaxiStations.add(taxiStation);
		if(mPendingServiceRequests.isEmpty() == false)
		{
			for (TransportServiceRequestMessage taxiServiceRequestMessage : mPendingServiceRequests) 
			{
				if(taxiServiceRequestMessage.getData().isValid())
				{
					forwardRequest(taxiServiceRequestMessage, taxiStation);
				}
				else 
				{
					mPendingServiceRequests.remove(taxiServiceRequestMessage);
				}
			}
		}
	}
	
	private void processRequest(RegisterAsBusServiceMessage registerMessage)
	{
		NetworkAddress busService = registerMessage.getFrom();
		mBusServices.add(busService);
		if(mPendingServiceRequests.isEmpty() == false)
		{
			for (TransportServiceRequestMessage request : mPendingServiceRequests) 
			{
				if(request.getData().isValid())
				{
					forwardRequest(request, busService);
				}
				else 
				{
					mPendingServiceRequests.remove(request);
				}
			}
		}
	}
	
	private void forwardRequest(TransportServiceRequestMessage requestMessage, NetworkAddress toTransportStation)
	{
		logger.info("ForwardRequest() requestMessage " + requestMessage);
		
		TransportServiceRequestMessage forwardMessage = new TransportServiceRequestMessage
				(requestMessage.getData(), requestMessage.getFrom(), toTransportStation);
		network.sendMessage(forwardMessage);
	}
}
