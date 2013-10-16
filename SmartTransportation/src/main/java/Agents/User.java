package agents;
import java.util.Set;
import java.util.UUID;

import messageData.taxiServiceRequest.TaxiServiceRequest;
import messages.DestinationReachedMessage;
import messages.RequestDestinationMessage;
import messages.RequestTaxiServiceConfirmationMessage;
import messages.TakeMeToDestinationMessage;
import messages.TaxiServiceReplyMessage;
import messages.TaxiServiceRequestConfirmationMessage;
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
	private TaxiServiceRequest mCurrentTaxiServiceRequest;
	
	public User(UUID id, String name, Location startLocation, Location targetLocation, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		
		assert(id != null);
		assert(startLocation != null);
		assert(targetLocation != null);
		assert(mediatorNetworkAddress != null);
		
		logger.info("User() id " + id);
		
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
		logger.info("initialise() mStartLocation " + mStartLocation);
		logger.info("initialise() mTargetLocation " + mTargetLocation);
		
		initializeLocationService();
		sendTaxiRequestMessageToMediator();
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
	
	private void sendTaxiRequestMessageToMediator()
	{
		logger.info("sendTaxiRequestMessageToMediator()");
		
		assert(mCurrentTaxiServiceRequest == null);
		
		mCurrentTaxiServiceRequest = new TaxiServiceRequest(mStartLocation, getID());
		TaxiServiceRequestMessage myMessage = new TaxiServiceRequestMessage(mCurrentTaxiServiceRequest, 
				network.getAddress(), mMediatorAddress);
		network.sendMessage(myMessage);
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		mCurrentTaxiServiceRequest.incrementTime();
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null)
		{
			if(input instanceof RequestTaxiServiceConfirmationMessage)
			{
				// TODO make this smarter; we want to weight our option
				// before confirming the offers from any taxi station
				if(mCurrentTaxiServiceRequest.isValid())
				{
					confirmRequest((RequestTaxiServiceConfirmationMessage)input);
				}
			}
			else if(input instanceof TaxiServiceReplyMessage)
			{
				processReply((TaxiServiceReplyMessage)input);
			}
			else if(input instanceof RequestDestinationMessage)
			{
				processRequest((RequestDestinationMessage)input);
			}
			else if(input instanceof DestinationReachedMessage)
			{
				onDestinationReached((DestinationReachedMessage)input);
			}
		}
	}
	
	private void confirmRequest(RequestTaxiServiceConfirmationMessage requestConfirmationMessage)
	{
		logger.info("confirmRequest() Taxi at: " + requestConfirmationMessage.getData());
		
		TaxiServiceRequestConfirmationMessage confirmationMessage = new TaxiServiceRequestConfirmationMessage("I confirm the request",
				network.getAddress(), requestConfirmationMessage.getFrom());
		network.sendMessage(confirmationMessage);
		mCurrentTaxiServiceRequest.cancel();
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
	
	private void onDestinationReached(DestinationReachedMessage message)
	{
		logger.info("onDestinationReached() " + message.getData());
		
	}
}
