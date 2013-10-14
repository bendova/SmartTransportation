package agents;
import java.util.Set;
import java.util.UUID;

import messageData.TaxiServiceRequest;
import messages.RequestDestinationMessage;
import messages.TakeMeToDestinationMessage;
import messages.TaxiServiceReplyMessage;
import messages.TaxiServiceRequestMessage;


import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;

public class User extends AbstractParticipant
{
	private Location mStartLocation;
	private Location mTargetLocation;
	private ParticipantLocationService mLocationService;
	private NetworkAddress mMediatorAddress;
	
	public User(UUID id, String name, Location startLocation, Location targetLocation, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		
		assert(id != null);
		assert(startLocation != null);
		assert(targetLocation != null);
		assert(mediatorNetworkAddress != null);
		
		mStartLocation = startLocation;
		mTargetLocation = targetLocation;
		mMediatorAddress = mediatorNetworkAddress;
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> shareState =  super.getSharedState();
		shareState.add(ParticipantLocationService.createSharedState(getID(), mStartLocation));
		return shareState;
	}
	
	@Override
	public void initialise()
	{
		super.initialise();
		
		initializeLocationService();
		sendRequestMessageToMediator();
	}
	
	private void initializeLocationService()
	{
		try
		{
			mLocationService = getEnvironmentService(ParticipantLocationService.class);
		}
		catch (UnavailableServiceException e) 
		{
			logger.warn(e);
		}
	}
	
	private void sendRequestMessageToMediator()
	{
		logger.info("SendRequestMessageToMediator() " + getTime());
		
		TaxiServiceRequest myRequest = new TaxiServiceRequest(mStartLocation);
		TaxiServiceRequestMessage myMessage = new TaxiServiceRequestMessage(myRequest, 
				network.getAddress(), mMediatorAddress);
		network.sendMessage(myMessage);
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
//		logger.info("incrementTime() " + getTime());
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null)
		{
			if(input instanceof TaxiServiceReplyMessage)
			{
				processReply((TaxiServiceReplyMessage)input);
			}
			else if(input instanceof RequestDestinationMessage)
			{
				processRequest((RequestDestinationMessage)input);
			}
		}
	}
	
	private void processReply(TaxiServiceReplyMessage taxiServiceReplyMessage)
	{
		logger.info("ProcessReply() Received reply: " + taxiServiceReplyMessage.getData().getMessage()); 
	}
	
	private void processRequest(RequestDestinationMessage requestDestinationMessage)
	{
		logger.info("processRequest() " + requestDestinationMessage.getData());
		
		TakeMeToDestinationMessage destinationMessage = new 
				TakeMeToDestinationMessage(mTargetLocation, network.getAddress(),
						requestDestinationMessage.getFrom());
		network.sendMessage(destinationMessage);
	}
}
