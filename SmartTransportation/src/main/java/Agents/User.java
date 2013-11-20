package agents;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import conversations.protocols.user.ProtocolWithTaxi;
import conversations.usertaxi.actions.DestinationReachedAction;
import conversations.usertaxi.actions.RequestDestinationAction;
import SmartTransportation.Simulation;
import messages.DestinationReachedMessage;
import messages.RequestDestinationMessage;
import messages.RequestTaxiServiceConfirmationMessage;
import messages.TakeMeToDestinationMessage;
import messages.TaxiServiceReplyMessage;
import messages.TaxiServiceRequestConfirmationMessage;
import messages.TaxiServiceRequestMessage;
import messages.messageData.taxiServiceRequest.TaxiServiceRequest;
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
	private boolean mDestinationReached;
	private boolean mLookingForTaxi;
	private ParticipantLocationService mLocationService;
	private NetworkAddress mMediatorAddress;
	private TaxiServiceRequest mCurrentTaxiServiceRequest;
	
	private Queue<RequestTaxiServiceConfirmationMessage> confirmationRequests;
	private ArrayList<Location> mLocations; 
	
	private ProtocolWithTaxi mWithTaxi;
	
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
		mDestinationReached = false;
		mLookingForTaxi = true;
		mMediatorAddress = mediatorNetworkAddress;
		
		confirmationRequests = new LinkedList<RequestTaxiServiceConfirmationMessage>();
		mLocations = new ArrayList<Location>();
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
		initialiseProtocol();
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
	
	private void initialiseProtocol()
	{
		RequestDestinationAction requestDestinationAction = new RequestDestinationAction()
		{
			@Override
			public void processMessage(RequestDestinationMessage msg) 
			{
				processRequest(msg);
			}
		};
		DestinationReachedAction destinationReachedAction = new DestinationReachedAction() 
		{
			@Override
			public void processMessage(DestinationReachedMessage msg)
			{
				onDestinationReached(msg);
			}
		};
		
		mWithTaxi = new ProtocolWithTaxi(network);
		mWithTaxi.init(requestDestinationAction, destinationReachedAction);
	}
	
	private void sendTaxiRequestMessageToMediator()
	{
		logger.info("sendTaxiRequestMessageToMediator()");
		
		assert(mCurrentTaxiServiceRequest == null);
		
		mCurrentTaxiServiceRequest = new TaxiServiceRequest(mStartLocation, getID(), authkey);
		TaxiServiceRequestMessage myMessage = new TaxiServiceRequestMessage(mCurrentTaxiServiceRequest, 
				network.getAddress(), mMediatorAddress);
		network.sendMessage(myMessage);
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		mCurrentTaxiServiceRequest.incrementTime();
		if(mDestinationReached == false)
		{
			mLocations.add(mLocationService.getAgentLocation(getID()));
		}
	}
	
	@Override
	public void execute()
	{
		// pull in Messages from the network
		enqueueInput(this.network.getMessages());
		
		// process inputs
		while (inputQueue.size() > 0) 
		{
			Input input = inputQueue.poll();
			if(input instanceof RequestTaxiServiceConfirmationMessage)
			{
				if(mLookingForTaxi)
				{
					confirmationRequests.add((RequestTaxiServiceConfirmationMessage)input);
				}
			}
			else
			{
				processInput(input);
			}
		}
		if(confirmationRequests.size() > 0)
		{
			handleConfirmationRequests();
			confirmationRequests.clear();
		}
	}
	
	private void handleConfirmationRequests()
	{
		logger.info("handleConfirmationRequests()");
		
		// let's find the offer for a taxi 
		// that is closest to us
		RequestTaxiServiceConfirmationMessage confirmedRequest = confirmationRequests.poll();
		double minDistance = confirmedRequest.getData().distanceTo(mStartLocation);
		for(RequestTaxiServiceConfirmationMessage request : confirmationRequests)
		{
			double distance = request.getData().distanceTo(mStartLocation);
			if(minDistance > distance)
			{
				confirmedRequest = request;
				minDistance = distance;
			}
		}
		
		logger.info("handleConfirmationRequests() Accepting offer for taxi at " + confirmedRequest.getData());
		
		mLookingForTaxi = false;		
		confirmRequest(confirmedRequest);
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
				//processRequest((RequestDestinationMessage)input);
				
				mWithTaxi.handleRequestDestination((RequestDestinationMessage)input);
			}
			else if(input instanceof DestinationReachedMessage)
			{
				//onDestinationReached((DestinationReachedMessage)input);
				
				mWithTaxi.handleOnDestinationReached((DestinationReachedMessage)input);
			}
		}
	}
	
	private void confirmRequest(RequestTaxiServiceConfirmationMessage requestConfirmationMessage)
	{
		logger.info("confirmRequest()");
		
		TaxiServiceRequestConfirmationMessage confirmationMessage = new TaxiServiceRequestConfirmationMessage("I confirm the request",
				network.getAddress(), requestConfirmationMessage.getFrom());
		network.sendMessage(confirmationMessage);
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
		//network.sendMessage(destinationMessage);
		mWithTaxi.sendTakeMeToDestination(destinationMessage);
		
		mCurrentTaxiServiceRequest.cancel();
	}
	
	private void onDestinationReached(DestinationReachedMessage message)
	{
		logger.info("onDestinationReached() " + message.getData());
		mDestinationReached = true;
	}
	
	@Override
	public void onSimulationComplete()
	{
		if(mDestinationReached == false)
		{
			logger.info("I didn't reach my destination!");
		}
		
		Simulation.addUserLocations(getName(), mLocations);
	}
}
