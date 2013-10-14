package Agents;
import java.util.Set;
import java.util.UUID;

import Messages.TaxiServiceReplyMessage;
import Messages.TaxiServiceRequest;
import Messages.TaxiServiceRequestMessage;

import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;

public class User extends AbstractParticipant
{
	private Location mLocation;
	private ParticipantLocationService mLocationService;
	private NetworkAddress mMediatorAddress;
	
	public User(UUID id, String name, Location location, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		
		mLocation = location;
		mMediatorAddress = mediatorNetworkAddress;
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> shareState =  super.getSharedState();
		shareState.add(ParticipantLocationService.createSharedState(getID(), mLocation));
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
		
		TaxiServiceRequest myRequest = new TaxiServiceRequest(mLocation);
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
	protected void processInput(Input in) 
	{
		if((in != null) && (in instanceof TaxiServiceReplyMessage))
		{
			processReply((TaxiServiceReplyMessage)in);
		}
	}
	
	private void processReply(TaxiServiceReplyMessage taxiServiceReplyMessage)
	{
		logger.info("ProcessReply() Received reply: " + taxiServiceReplyMessage.getData().getMessage()); 
	}
	
}
