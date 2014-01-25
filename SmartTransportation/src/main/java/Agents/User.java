package agents;
import java.util.*;

import conversations.protocols.user.ProtocolWithTaxi;
import conversations.userBus.messages.NotificationOfArrivalAtBusStop;
import conversations.userBusStation.*;
import conversations.userMediator.messages.*;
import conversations.userTaxi.actions.*;
import conversations.userTaxi.messages.*;
import SmartTransportation.Simulation;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.Move;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;
import uk.ac.imperial.presage2.util.participant.HasPerceptionRange;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;

public class User extends AbstractParticipant implements HasPerceptionRange
{
	enum STATE
	{
		LOOKING_FOR_TRANSPORT,
		TRAVELING_BY_TAXI,
		TRAVELING_TO_BUS_STOP,
		TRAVELING_BY_BUS,
		TRAVELING_TO_DESTINATION,
		REACHED_DESTINATION
	}
	
	private STATE mCurrentState;
	
	private Location mCurrentLocation;
	private Location mStartLocation;
	private Location mTargetLocation;
	private ParticipantLocationService mLocationService;
	private NetworkAddress mMediatorAddress;
	private TransportServiceRequest mCurrentServiceRequest;
	
	private Queue<RequestTaxiConfirmationMessage> mConfirmationRequests;
	private ArrayList<Location> mTraveledLocations; 
	
	private IBusTravelPlan mBusTravelPlan;
	private int mCurrentPathIndex;
	
	private ProtocolWithTaxi mWithTaxi;
	
	public User(UUID id, String name, Location startLocation, Location targetLocation, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		
		assert(id != null);
		assert(startLocation != null);
		assert(targetLocation != null);
		assert(mediatorNetworkAddress != null);
		
		logger.info("User() id " + id);
		
		mCurrentState = STATE.LOOKING_FOR_TRANSPORT;
		mCurrentLocation = startLocation;
		mStartLocation = startLocation;
		mTargetLocation = targetLocation;
		mMediatorAddress = mediatorNetworkAddress;
		
		mConfirmationRequests = new LinkedList<RequestTaxiConfirmationMessage>();
		mTraveledLocations = new ArrayList<Location>();
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> shareState = super.getSharedState();
		shareState.add(ParticipantLocationService.createSharedState(getID(), mCurrentLocation));
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
	
	private void sendRequestMessageToMediator()
	{
		logger.info("sendTaxiRequestMessageToMediator()");
		
		assert(mCurrentServiceRequest == null);
		
		mCurrentServiceRequest = new TransportServiceRequest(mStartLocation, mTargetLocation,
				getID(), authkey);
		TransportServiceRequestMessage myMessage = new TransportServiceRequestMessage
				(mCurrentServiceRequest, network.getAddress(), mMediatorAddress);
		network.sendMessage(myMessage);
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		mCurrentServiceRequest.incrementTime();
		updateLocation();
		
		switch (mCurrentState) 
		{
		case TRAVELING_TO_BUS_STOP:
			if(mCurrentPathIndex < mBusTravelPlan.getPathToFirstBusStop().size())
			{
				moveTo(mBusTravelPlan.getPathToFirstBusStop().get(mCurrentPathIndex++));
			}
			else
			{
				mCurrentState = STATE.TRAVELING_BY_BUS;
				mCurrentPathIndex = 0;
			}
			break;
		case TRAVELING_TO_DESTINATION:
			if(mCurrentPathIndex < mBusTravelPlan.getPathToDestination().size())
			{
				moveTo(mBusTravelPlan.getPathToDestination().get(mCurrentPathIndex++));
			}
			else 
			{
				mCurrentState = STATE.REACHED_DESTINATION;
			}
			break;
		default:
			break;
		}
	}
	
	private void updateLocation()
	{
		Location currentLocation = mLocationService.getAgentLocation(getID());
		if(mCurrentLocation.equals(currentLocation) == false)
		{
			logger.info("updateLocation() currentLocation " + currentLocation);
			
			mCurrentLocation = currentLocation;
		}
		mTraveledLocations.add(currentLocation);
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
			if(input instanceof RequestTaxiConfirmationMessage)
			{
				if(mCurrentState == STATE.LOOKING_FOR_TRANSPORT)
				{
					mConfirmationRequests.add((RequestTaxiConfirmationMessage)input);
				}
			}
			else
			{
				processInput(input);
			}
		}
		if(mConfirmationRequests.size() > 0)
		{
			handleConfirmationRequests();
			mConfirmationRequests.clear();
		}
	}
	
	private void handleConfirmationRequests()
	{
		logger.info("handleConfirmationRequests()");
		
		// let's find the offer for a taxi 
		// that is closest to us
		RequestTaxiConfirmationMessage confirmedRequest = mConfirmationRequests.poll();
		double minDistance = confirmedRequest.getData().distanceTo(mStartLocation);
		for(RequestTaxiConfirmationMessage request : mConfirmationRequests)
		{
			double distance = request.getData().distanceTo(mStartLocation);
			if(minDistance > distance)
			{
				confirmedRequest = request;
				minDistance = distance;
			}
		}
		
		logger.info("handleConfirmationRequests() Accepting offer for taxi at " + confirmedRequest.getData());
		
		mCurrentState = STATE.TRAVELING_BY_TAXI;
		confirmRequest(confirmedRequest);
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null)
		{
			if(input instanceof TaxiReplyMessage)
			{
				processReply((TaxiReplyMessage)input);
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
			else if (input instanceof BusTravelPlanMessage)
			{
				handleBusTravelPlan((BusTravelPlanMessage)input);
			}
			else if(input instanceof NotificationOfArrivalAtBusStop)
			{
				handleBusArrival((NotificationOfArrivalAtBusStop)input);
			}
		}
	}
	
	private void confirmRequest(RequestTaxiConfirmationMessage requestConfirmationMessage)
	{
		logger.info("confirmRequest()");
		
		TaxiRequestConfirmationMessage confirmationMessage = new TaxiRequestConfirmationMessage("I confirm the request",
				network.getAddress(), requestConfirmationMessage.getFrom());
		network.sendMessage(confirmationMessage);
	}
	
	private void processReply(TaxiReplyMessage taxiServiceReplyMessage)
	{
		logger.info("ProcessReply() Received reply: " + taxiServiceReplyMessage.getData().getMessage()); 
	}
	
	private void processRequest(RequestDestinationMessage requestDestinationMessage)
	{
		logger.info("processRequest() " + requestDestinationMessage.getData());
		
		TakeMeToDestinationMessage destinationMessage = new 
				TakeMeToDestinationMessage(mTargetLocation, network.getAddress(),
						requestDestinationMessage.getFrom());
		mWithTaxi.sendTakeMeToDestination(destinationMessage);
		
		mCurrentServiceRequest.cancel();
	}
	
	private void onDestinationReached(DestinationReachedMessage message)
	{
		logger.info("onDestinationReached() " + message.getData());
		mCurrentState = STATE.REACHED_DESTINATION;
	}
	
	private void handleBusTravelPlan(BusTravelPlanMessage msg)
	{
		if(mCurrentState == STATE.LOOKING_FOR_TRANSPORT)
		{
			mBusTravelPlan = msg.getData();
			mCurrentPathIndex = 0;
			
			logger.info("handleBusTravelPlan() mBusTravelPlan.getPathToFirstBusStop() " + mBusTravelPlan.getPathToFirstBusStop());
			logger.info("handleBusTravelPlan() mBusTravelPlan.getPathToDestination() " + mBusTravelPlan.getPathToDestination());
			
			moveTo(mBusTravelPlan.getPathToFirstBusStop().get(mCurrentPathIndex++));
			mCurrentState = STATE.TRAVELING_TO_BUS_STOP;
		}
	}
	
	private void handleBusArrival(NotificationOfArrivalAtBusStop notification)
	{
		if(notification.getData().getBusStopLocation().equals(mCurrentLocation))
		{
			logger.info("handleBusArrival() The bus has arrived " + notification.getFrom());
		}
	}
	
	private void moveTo(Location target)
	{
		Move move = new Move(mCurrentLocation.getMoveTo(target));
		try 
		{
			environment.act(move, getID(), authkey);
		}
		catch (ActionHandlingException e) 
		{
			logger.warn("Error while moving!", e);
		}
	}
	
	@Override
	public void onSimulationComplete()
	{
		if(mCurrentState != STATE.REACHED_DESTINATION)
		{
			logger.info("I didn't reach my destination!");
		}
		
		Simulation.addUserLocations(getName(), mTraveledLocations);
	}

	@Override
	public double getPerceptionRange() 
	{
		return 1;
	}
}
