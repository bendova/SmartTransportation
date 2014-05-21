package agents;

import java.util.*;

import SmartTransportation.Simulation.TransportMethodSpeed;

import map.CityMap;

import conversations.protocols.user.ProtocolWithTaxi;
import conversations.userBus.messages.BoardBusRequestMessage;
import conversations.userBus.messages.BusBoardingSuccessfulMessage;
import conversations.userBus.messages.BusIsFullMessage;
import conversations.userBus.messages.BusUnBoardingSuccessful;
import conversations.userBus.messages.NotificationOfArrivalAtBusStop;
import conversations.userBus.messages.UnBoardBusRequestMessage;
import conversations.userBus.messages.messageData.BoardBusRequest;
import conversations.userBusStation.*;
import conversations.userMediator.messages.*;
import conversations.userMediator.messages.messageData.TransportServiceRequest;
import conversations.userTaxi.actions.*;
import conversations.userTaxi.messages.*;
import dataStores.SimulationDataStore;
import dataStores.UserDataStore;
import transportOffers.BusTransportOffer;
import transportOffers.ITransportOffer;
import transportOffers.WalkTransportOffer;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;
import uk.ac.imperial.presage2.util.participant.HasPerceptionRange;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import util.movement.TransportMove;

public class User extends AbstractParticipant implements HasPerceptionRange
{
	public enum State
	{
		INITIAL,
		LOOKING_FOR_TRANSPORT,
		WAITING_FOR_TAXI,
		TRAVELING_BY_TAXI,
		TRAVELING_TO_BUS_STOP,
		IN_BUS_STOP,
		WAITING_BUS_BOARD_CONFIRMATION,
		TRAVELING_BY_BUS,
		WAITING_BUS_UNBOARD_CONFIRMATION,
		TRAVELING_ON_FOOT,
		REACHED_DESTINATION
	}
	
	public enum TransportPreference
	{
		NO_PREFERENCE 		(1, 1, 1),
		WALKING_PREFERENCE	(1, 3, 5),
		BUS_PREFERENCE		(5, 1, 3),
		TAXI_PREFERENCE		(5, 3, 1);
		
		int mWalkingCostScaling;
		int mBusCostScaling;
		int mTaxiCostScaling;
		private TransportPreference(int walkingCostScaling, int busCostScaling, int taxiCostScaling)
		{
			mWalkingCostScaling = walkingCostScaling;
			mBusCostScaling = busCostScaling;
			mTaxiCostScaling = taxiCostScaling;
		}
		public int getWalkingCostScaling()
		{
			return mWalkingCostScaling;
		}
		public int getBusCostScaling()
		{
			return mBusCostScaling;
		}
		public int getTaxiCostScaling()
		{
			return mTaxiCostScaling;
		}
	}
	
	public enum TransportSortingPreference
	{
		PREFER_CHEAPEST,
		PREFER_FASTEST
	}
	
	public enum TransportMode
	{
		NONE		("None"),
		WALKING		("Walking"),
		TAKE_TAXI	("Taxi"),
		TAKE_BUS	("Bus");
		
		private String mName;
		private TransportMode(String name)
		{
			mName = name;
		}
		public String getName()
		{
			return mName;
		}
	}
	
	private State mCurrentState;
	private TransportPreference mTransportPreference;
	private TransportSortingPreference mTransportSortingPreference;
	private TransportMode mTransportModeUsed;
	
	private Location mCurrentLocation;
	private Location mStartLocation;
	private Location mTargetLocation;
	private int mTravelTimeTarget;
	private int mTravelTime;
	private ParticipantLocationService mLocationService;
	private NetworkAddress mMediatorAddress;
	private TransportServiceRequest mCurrentServiceRequest;
	
	private IBusTravelPlan mBusTravelPlan;
	
	// the path that we are currently following on foot
	private List<Location> mOnFootTravelPath;
	
	private int mTimeTakenPerUnitDistance;
	private ProtocolWithTaxi mWithTaxi;
	
	private CityMap mCityMap;
	private SimulationDataStore mSimulationDataStore;
	
	private List<ITransportOffer> mReceivedTransportOffers;
	
	public User(UUID id, String name, CityMap cityMap, Location startLocation, Location targetLocation, 
			int travelTimeTarget, NetworkAddress mediatorNetworkAddress, TransportPreference transportPreference) 
	{
		super(id, name);
		
		assert(id != null);
		assert(cityMap != null);
		assert(startLocation != null);
		assert(targetLocation != null);
		assert(travelTimeTarget >= 0);
		assert(mediatorNetworkAddress != null);
		
		logger.info("User() id " + id);
		logger.info("User() transportPreference " + transportPreference);
		
		mCurrentState = State.INITIAL;
		mTransportPreference = transportPreference;
		
		// TODO pass this preference as a parameter
		mTransportSortingPreference = TransportSortingPreference.PREFER_FASTEST;
		mTransportModeUsed = TransportMode.NONE;
		mCityMap = cityMap;
		mCurrentLocation = startLocation;
		mStartLocation = startLocation;
		mTargetLocation = targetLocation;
		mTravelTimeTarget = travelTimeTarget;
		mMediatorAddress = mediatorNetworkAddress;
		
		mTravelTime = 0;
		mTimeTakenPerUnitDistance = TransportMethodSpeed.WALKING_SPEED.getTimeTakenPerUnitDistance();
		
		mReceivedTransportOffers = new ArrayList<ITransportOffer>();
	}
	
	public void setDataStore(SimulationDataStore dataStore) 
	{
		assert(dataStore != null);
		mSimulationDataStore = dataStore;
	}
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> shareState = super.getSharedState();
		shareState.add(ParticipantLocationService.createSharedState(getID(), mCurrentLocation));
		return shareState;
	}
	
	public double getTravelTimeTarget()
	{
		return mTravelTimeTarget;
	}
	
	public double getTravelTime()
	{
		return mTravelTime;
	}
	
	public State getCurrentState()
	{
		return mCurrentState;
	}
	
	@Override
	public void initialise()
	{
		super.initialise();
		logger.info("initialise() mStartLocation " + mStartLocation);
		logger.info("initialise() mTargetLocation " + mTargetLocation);
		
		initializeLocationService();
		initialiseProtocol();
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
				logger.info("onDestinationReached() " + msg.getData());
			}
		};
		
		mWithTaxi = new ProtocolWithTaxi(network);
		mWithTaxi.init(requestDestinationAction, destinationReachedAction);
	}
	
	public void sendRequestMessageToMediator()
	{
		logger.info("sendRequestMessageToMediator()");
		
		assert(mCurrentServiceRequest == null);
		
		mCurrentServiceRequest = new TransportServiceRequest(mStartLocation, mTargetLocation, 
				mTravelTimeTarget, mTransportPreference, mTransportSortingPreference, getID(), authkey,
				network.getAddress());
		TransportServiceRequestMessage myMessage = new TransportServiceRequestMessage
				(mCurrentServiceRequest, network.getAddress(), mMediatorAddress);
		network.sendMessage(myMessage);
		
		mCurrentState = State.LOOKING_FOR_TRANSPORT;		
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		mCurrentServiceRequest.incrementTime();
		updateLocation();

		if(mCurrentState != State.REACHED_DESTINATION)
		{
			++mTravelTime;
		}
		
		switch (mCurrentState) 
		{
		case TRAVELING_TO_BUS_STOP:
			if(mBusTravelPlan.getPathToFirstBusStop().size() > 0)
			{
				mTimeTakenPerUnitDistance = TransportMethodSpeed.WALKING_SPEED.getTimeTakenPerUnitDistance();
				moveTo(mBusTravelPlan.getPathToFirstBusStop().remove(0));
			}
			else
			{
				mCurrentState = State.IN_BUS_STOP;
			}
			break;
			
		case TRAVELING_ON_FOOT:
			if(mOnFootTravelPath.size() > 0)
			{
				mTimeTakenPerUnitDistance = TransportMethodSpeed.WALKING_SPEED.getTimeTakenPerUnitDistance();
				moveTo(mOnFootTravelPath.remove(0));
			}
			break;

		case TRAVELING_BY_BUS:
			mTimeTakenPerUnitDistance = TransportMethodSpeed.BUS_SPEED.getTimeTakenPerUnitDistance();				
			break;
			
		case TRAVELING_BY_TAXI:
			mTimeTakenPerUnitDistance = TransportMethodSpeed.TAXI_SPEED.getTimeTakenPerUnitDistance();				
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
			if(mCurrentLocation.equals(mTargetLocation))
			{
				onDestinationReached();
			}
		}
	}
	
	public List<ITransportOffer> getReceivedTransportOffers()
	{
		return mReceivedTransportOffers;
	}
	
	public void selectTransportOffer(ITransportOffer selectedTransportOffer)
	{
		logger.info("selectTransportOffer() I am choosing this offer: " + selectedTransportOffer);
		
		assert(mCurrentState == State.LOOKING_FOR_TRANSPORT);
		
		switch (selectedTransportOffer.getTransportMode()) 
		{
		case WALKING:
			logger.info("selectTransportOffer() I am walking to my destination.");
			
			// walk there
			mTransportModeUsed = TransportMode.WALKING;
			mCurrentState = State.TRAVELING_ON_FOOT;
			mOnFootTravelPath = ((WalkTransportOffer)selectedTransportOffer).getWalkPath();
			break;
		case TAKE_BUS:
			logger.info("selectTransportOffer() I am taking the bus to my destination.");
			
			// take the bus
			mTransportModeUsed = TransportMode.TAKE_BUS;
			mCurrentState = State.TRAVELING_TO_BUS_STOP;
			handleBusTravelPlan(((BusTransportOffer)selectedTransportOffer).getBusTravelPlanMessage());
			break;
		case TAKE_TAXI:
			logger.info("selectTransportOffer() I am taking the taxi to my destination");
			
			// take the taxi
			mTransportModeUsed = TransportMode.TAKE_TAXI;
			mCurrentState = State.WAITING_FOR_TAXI;
			break;
		default:
			assert(false) : "selectTransportOffer(): Unhandled transport offer: " + selectedTransportOffer.getTransportMode();
			break;
		}
		
		sendConfirmationMessage(selectedTransportOffer);
		clearTransportOffers();
	}
	
	private void clearTransportOffers()
	{
		mReceivedTransportOffers = new ArrayList<ITransportOffer>();
	}
	
	private void sendConfirmationMessage(ITransportOffer selectedTransportOffer)
	{
		ConfirmTransportOfferMessage msg = new ConfirmTransportOfferMessage(selectedTransportOffer, 
				network.getAddress(), mMediatorAddress);
		network.sendMessage(msg);
	}
	
	private void logTransportOffers(List<ITransportOffer> transportOffers)
	{
		logger.info("logTransportOffers() I need to get there in: " + mTravelTimeTarget + " time units.");
		
		int timeLeft = mTravelTimeTarget - mTravelTime;
		if(timeLeft > 0)
		{
			logger.info("logTransportOffers() I have left: " + timeLeft + " time units.");
		}
		else
		{
			logger.info("logTransportOffers() I am late by: " + (-timeLeft) + " time units.");
		}
		
		for (int i = 0; i < transportOffers.size(); ++i) 
		{
			ITransportOffer offer = transportOffers.get(i);
			logger.info("logTransportOffers() " + offer.getTransportMode() + 
					" costs: " + offer.getCost() + " currency units");
			logger.info("logTransportOffers() " + offer.getTransportMode() + 
					" takes: " + offer.getTravelTime() + " time units");
		}
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(mCurrentState == State.LOOKING_FOR_TRANSPORT)
		{
			if(input instanceof TransportServiceOfferMessage)
			{
				mReceivedTransportOffers = ((TransportServiceOfferMessage)input).getData().getTransportOffers();
				logTransportOffers(mReceivedTransportOffers);
			}
		}
		else 
		{
			switch(mTransportModeUsed)
			{
			case TAKE_TAXI:
				processTaxiMessage(input);
				break;
			case TAKE_BUS:
				processBusMessage(input);
				break;
			}
		}
	}
	
	private void processTaxiMessage(Input input)
	{
		if(input instanceof TaxiReplyMessage)
		{
			processReply((TaxiReplyMessage)input);
		}
		else if(input instanceof RequestDestinationMessage)
		{
			mWithTaxi.handleRequestDestination((RequestDestinationMessage)input);
		}
		else if(input instanceof DestinationReachedMessage)
		{
			mWithTaxi.handleOnDestinationReached((DestinationReachedMessage)input);
		}
	}
	
	private void processBusMessage(Input input)
	{
		if(input instanceof NotificationOfArrivalAtBusStop)
		{
			handleArrivalAtBusStop((NotificationOfArrivalAtBusStop)input);
		}
		else if(input instanceof BusBoardingSuccessfulMessage)
		{
			handleBusBoardSuccesful((BusBoardingSuccessfulMessage)input);
		}
		else if(input instanceof BusIsFullMessage)
		{
			handleBusBoardFailure((BusIsFullMessage)input);
		}
		else if(input instanceof BusUnBoardingSuccessful)
		{
			handleBusUnBoarded((BusUnBoardingSuccessful)input);
		}
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
		
		mCurrentState = State.TRAVELING_BY_TAXI;
	}
	
	private void onDestinationReached()
	{	
		logger.info("onDestinationReached()");

		mCurrentState = State.REACHED_DESTINATION;
	}
	
	private void handleBusTravelPlan(BusTravelPlanMessage msg)
	{
		logger.info("handleBusTravelPlan() mCurrentState " + mCurrentState);
		
		mBusTravelPlan = msg.getData();
		
		logger.info("handleBusTravelPlan() mBusTravelPlan.getPathToFirstBusStop() " + mBusTravelPlan.getPathToFirstBusStop());
		logger.info("handleBusTravelPlan() mBusTravelPlan.getPathToDestination() " + mBusTravelPlan.getPathToDestination());
	}
	
	private void handleArrivalAtBusStop(NotificationOfArrivalAtBusStop notification)
	{		
		if(notification.getData().getBusRoute().getBusRouteID().
				equals(mBusTravelPlan.getBusRouteID()))
		{
			switch (mCurrentState) 
			{
				case IN_BUS_STOP:
				{
					logger.info("handleBusArrival() The bus has arrived " + notification.getFrom());
					
					mCurrentState = State.WAITING_BUS_BOARD_CONFIRMATION;
					
					// send a request to board the bus
					BoardBusRequest request = new BoardBusRequest(authkey);
					BoardBusRequestMessage msg = new BoardBusRequestMessage(request, 
							network.getAddress(), notification.getFrom());
					network.sendMessage(msg);	
				}
				break;
				case TRAVELING_BY_BUS:
				{
					Location targetBusStop = mBusTravelPlan.getDestinationBusStopLocation();
					Location currentBusStop = notification.getData().getBusStopLocation();
					if(currentBusStop.equals(targetBusStop))
					{
						logger.info("handleBusArrival() I've reached my bus stop " + targetBusStop);
						
						mCurrentState = State.WAITING_BUS_UNBOARD_CONFIRMATION;
						// send a request to unboard the bus
						String request = "This is my stop!";
						UnBoardBusRequestMessage msg = new UnBoardBusRequestMessage(request, 
								network.getAddress(), notification.getFrom());
						network.sendMessage(msg);
					}
				}
				break;
			}
		}
	}
	
	private void handleBusBoardSuccesful(BusBoardingSuccessfulMessage msg)
	{
		logger.info("handleBusBoardSuccesful() I've boarded the bus " + msg.getFrom());
		
		assert (mCurrentState == State.WAITING_BUS_BOARD_CONFIRMATION);
		
		mCurrentState = State.TRAVELING_BY_BUS;
	}
	
	private void handleBusBoardFailure(BusIsFullMessage msg)
	{
		logger.info("handleBusBoardFailure() This bus is full " + msg.getFrom());
		
		assert (mCurrentState == State.WAITING_BUS_BOARD_CONFIRMATION);
		
		mCurrentState = State.IN_BUS_STOP;
	}
	
	private void handleBusUnBoarded(BusUnBoardingSuccessful msg)
	{
		logger.info("handleBusUnBoarded() I've got of the bus " + msg.getFrom());
		
		assert (mCurrentState == State.WAITING_BUS_UNBOARD_CONFIRMATION);
		
		mCurrentState = State.TRAVELING_ON_FOOT;
		mOnFootTravelPath = mBusTravelPlan.getPathToDestination();
	}
	
	private void moveTo(Location target)
	{
		TransportMove move = new TransportMove(target, 
				mTimeTakenPerUnitDistance);
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
		UUID userID = getID();
		UserDataStore userDataStore = new UserDataStore(getName(), userID, mStartLocation);
		if(mCurrentState != State.REACHED_DESTINATION)
		{
			logger.info("I didn't reach my destination!");
			
			userDataStore.setHasReachedDestination(false);
		}
		else 
		{
			userDataStore.setHasReachedDestination(true);
		}
		userDataStore.setTransportPreference(mTransportPreference);
		userDataStore.setTransportMethodUsed(mTransportModeUsed);
		userDataStore.setTargetTravelTime(mTravelTimeTarget);
		userDataStore.setActualTravelTime(mTravelTime);
		
		mSimulationDataStore.addUserDataStore(userID, userDataStore);
	}

	@Override
	public double getPerceptionRange() 
	{
		return 1;
	}
}
