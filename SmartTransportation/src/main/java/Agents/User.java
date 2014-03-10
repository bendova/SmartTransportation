package agents;

import java.util.*;

import SmartTransportation.Simulation.TransportMethodCost;
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
import conversations.userTaxi.actions.*;
import conversations.userTaxi.messages.*;
import dataStores.SimulationDataStore;
import dataStores.UserDataStore;
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
	private enum STATE
	{
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
	
	private class TransportOffer implements Comparable<TransportOffer>
	{
		private double mCost = Double.MAX_VALUE;
		private double mTimeTaken = Double.MAX_VALUE;
		private TransportMode mOfferType;
		
		public TransportOffer(TransportMode offerType)
		{
			mOfferType = offerType;
		}
		
		public TransportMode getOfferType()
		{
			return mOfferType;
		}
		
		public void setCost(double cost)
		{
			mCost = cost;
		}
		public void setTimeTaken(double time)
		{
			mTimeTaken = time;
		}
		public double getCost()
		{
			return mCost;
		}
		public double getTimeTaken()
		{
			return mTimeTaken;
		}

		@Override
		public int compareTo(TransportOffer obj) 
		{
			if (obj == null)
			{
				return 1;
			}
			else if (this == obj)
			{
				return 0;
			}
			else 
			{
				double costDiff = mCost - obj.mCost;
				if(costDiff < 0)
				{
					return -1;
				}
				else if(costDiff > 0)
				{
					return 1;
				}
				else 
				{
					return 0;
				}
			}
		}
	}
	
	private STATE mCurrentState;
	private TransportPreference mTransportPreference;
	private TransportMode mTransportModeUsed;
	private boolean mIsWalkingEnabled = true;
	private boolean mAreBusesEnabled = true;
	private boolean mAreTaxiesEnabled = true;
	
	private Location mCurrentLocation;
	private Location mStartLocation;
	private Location mTargetLocation;
	private double mTravelTimeTarget;
	private double mTravelTime;
	private ParticipantLocationService mLocationService;
	private NetworkAddress mMediatorAddress;
	private TransportServiceRequest mCurrentServiceRequest;
	
	private Queue<RequestTaxiConfirmationMessage> mTaxiConfirmationRequests;
	private Queue<BusTravelPlanMessage> mBusTravelPlanMessages;
	private ArrayList<Location> mPathTraveled;
	
	private IBusTravelPlan mBusTravelPlan;
	private int mCurrentPathIndex;
	
	// the path that we are currently following on foot
	private List<Location> mOnFootTravelPath;
	// the path that we must follow if we are going to 
	// out destination on foot
	private List<Location> mWalkingTravelPlan;
	
	private ProtocolWithTaxi mWithTaxi;
	
	private CityMap mCityMap;
	private SimulationDataStore mSimulationDataStore;
	private UserDataStore mUserDataStore;
	
	public User(UUID id, String name, CityMap cityMap, Location startLocation, Location targetLocation, 
			double travelTimeTarget, NetworkAddress mediatorNetworkAddress, TransportPreference transportPreference) 
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
		
		mCurrentState = STATE.LOOKING_FOR_TRANSPORT;
		mTransportPreference = transportPreference;
		mTransportModeUsed = TransportMode.NONE;
		mCityMap = cityMap;
		mCurrentLocation = startLocation;
		mStartLocation = startLocation;
		mTargetLocation = targetLocation;
		mTravelTimeTarget = travelTimeTarget;
		mMediatorAddress = mediatorNetworkAddress;
		
		mTaxiConfirmationRequests = new LinkedList<RequestTaxiConfirmationMessage>();
		mBusTravelPlanMessages = new LinkedList<BusTravelPlanMessage>();
		mPathTraveled = new ArrayList<Location>();
		mTravelTime = 0.0;
		
		mWalkingTravelPlan = mCityMap.getPath(mStartLocation, mTargetLocation);
		
		mUserDataStore = new UserDataStore(name, id);
	}
	public void enableWalking(boolean enable)
	{
		mIsWalkingEnabled = enable;
	}
	public void enableTaxiUse(boolean enable)
	{
		mAreTaxiesEnabled = enable;
	}
	public void enableBusUse(boolean enable)
	{
		mAreBusesEnabled = enable;
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
				logger.info("onDestinationReached() " + msg.getData());
				
				onDestinationReached();
			}
		};
		
		mWithTaxi = new ProtocolWithTaxi(network);
		mWithTaxi.init(requestDestinationAction, destinationReachedAction);
	}
	
	private void sendRequestMessageToMediator()
	{
		logger.info("sendRequestMessageToMediator()");
		
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
		case LOOKING_FOR_TRANSPORT:
		case WAITING_FOR_TAXI:
		case IN_BUS_STOP: // waiting for the bus
		case WAITING_BUS_BOARD_CONFIRMATION:
		case WAITING_BUS_UNBOARD_CONFIRMATION:
			++mTravelTime;
			break;
		case TRAVELING_BY_TAXI:
			mTravelTime += 1.0 / TransportMethodSpeed.TAXI_SPEED.getSpeed();
			break;
		case TRAVELING_TO_BUS_STOP:
			if(mCurrentPathIndex < mBusTravelPlan.getPathToFirstBusStop().size())
			{
				moveTo(mBusTravelPlan.getPathToFirstBusStop().get(mCurrentPathIndex++));
				mTravelTime += 1.0 / TransportMethodSpeed.BUS_SPEED.getSpeed();
			}
			else
			{
				mCurrentState = STATE.IN_BUS_STOP;
				++mTravelTime;
			}
			break;
		case TRAVELING_ON_FOOT:
			if(mCurrentPathIndex < mOnFootTravelPath.size())
			{
				moveTo(mOnFootTravelPath.get(mCurrentPathIndex++));
				mTravelTime += 1.0 / TransportMethodSpeed.TAXI_SPEED.getSpeed();
			}
			else 
			{
				mCurrentState = STATE.REACHED_DESTINATION;
			}
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
		mPathTraveled.add(currentLocation);
	}
	
	@Override
	public void execute()
	{
		if(mCurrentState == STATE.LOOKING_FOR_TRANSPORT)
		{
			// pull in Messages from the network
			enqueueInput(this.network.getMessages());
			
			while (inputQueue.size() > 0) 
			{
				Input input = inputQueue.poll();
				if(input instanceof RequestTaxiConfirmationMessage)
				{
					mTaxiConfirmationRequests.add((RequestTaxiConfirmationMessage)input);
				}
				else if(input instanceof BusTravelPlanMessage)
				{
					mBusTravelPlanMessages.add((BusTravelPlanMessage)input);
				}
				else
				{
					processInput(input);
				}
			}
			boolean canGo = false;
			if(mAreTaxiesEnabled || mAreBusesEnabled)
			{
				if((mTaxiConfirmationRequests.size() > 0) || (mBusTravelPlanMessages.size() > 0))
				{
					canGo = true;
				}
			}
			else if(mIsWalkingEnabled)
			{
				canGo = true;
			}
			if(canGo)
			{
				decideTransportMethod();
				
				mTaxiConfirmationRequests.clear();
				mBusTravelPlanMessages.clear();
			}
		}
		else
		{
			super.execute();
		}
	}
	
	private void decideTransportMethod()
	{
		logger.info("decideTransportMethod()");
		
		List<TransportOffer> transportOffers = new ArrayList<User.TransportOffer>();
		
		if(mIsWalkingEnabled)
		{
			if(mWalkingTravelPlan != null)
			{
				logger.info("decideTransportMethod() mWalkingTravelPlan " + mWalkingTravelPlan);
				
				double onFootTravelDistance = mWalkingTravelPlan.size();
				double walkingTravelTime = onFootTravelDistance / TransportMethodSpeed.WALKING_SPEED.getSpeed();
				double walkingTravelCost = onFootTravelDistance * TransportMethodCost.WALKING_COST.getCost();
				walkingTravelCost *= mTransportPreference.getWalkingCostScaling();
				
				TransportOffer walk = new TransportOffer(TransportMode.WALKING);
				walk.setTimeTaken(walkingTravelTime);
				walk.setCost(walkingTravelCost);
				
				transportOffers.add(walk);				
			}
		}
		
		RequestTaxiConfirmationMessage taxiOffer = null;
		if(mAreTaxiesEnabled)
		{
			taxiOffer = getBestTaxiOffer(mTaxiConfirmationRequests);		
			if(taxiOffer != null)
			{
				logger.info("decideTransportMethod() taxiOffer " + taxiOffer);
				
				double taxiTravelTime = taxiOffer.getData().getTotalTravelTime();
				double taxiTravelCost = taxiOffer.getData().getTravelCost();
				taxiTravelCost *= mTransportPreference.mTaxiCostScaling;
				
				TransportOffer taxi = new TransportOffer(TransportMode.TAKE_TAXI);
				taxi.setTimeTaken(taxiTravelTime);
				taxi.setCost(taxiTravelCost);
				transportOffers.add(taxi);
			}
		}
		
		BusTravelPlanMessage busOffer = null;
		if(mAreBusesEnabled)
		{
			busOffer = getBestBusPlan(mBusTravelPlanMessages);
			if(busOffer != null)
			{
				logger.info("decideTransportMethod() busOffer " + busOffer);
				
				int travelToBusStopDistance = busOffer.getData().getPathToFirstBusStop().size();
				int travelToDestinationDistance = busOffer.getData().getPathToDestination().size();
				double busTravelDistance = (double)busOffer.getData().getBusTravelDistance();
				// this is the worst case scenario - there is only one bus on the route,
				// and it has just left the station
				busTravelDistance *= 2;  
				double onFootTravelTime = (double)(travelToBusStopDistance + travelToDestinationDistance) 
						/ TransportMethodSpeed.WALKING_SPEED.getSpeed();
				double onBusTravelTime = busTravelDistance / TransportMethodSpeed.BUS_SPEED.getSpeed();
				double busTravelTime = onFootTravelTime + onBusTravelTime;
				double busTravelCost = busTravelDistance * TransportMethodCost.BUS_COST.getCost();
				busTravelCost *= mTransportPreference.getBusCostScaling();
				
				TransportOffer bus = new TransportOffer(TransportMode.TAKE_BUS);
				bus.setTimeTaken(busTravelTime);
				bus.setCost(busTravelCost);
				transportOffers.add(bus);
			}
		}
		
		if(transportOffers.size() > 0)
		{
			Collections.sort(transportOffers);
			
			logger.info("decideTransportMethod() I need to get there in: " + mTravelTimeTarget + " time units.");
			logTransportOffers(transportOffers);
			
			TransportOffer selectedTransportOffer = transportOffers.get(0);
			double minTravelTime = selectedTransportOffer.getTimeTaken();
			for (int i = 1; i < transportOffers.size(); ++i) 
			{
				double travelTime = transportOffers.get(i).getTimeTaken();
				if(travelTime < minTravelTime)
				{
					selectedTransportOffer = transportOffers.get(i);
					minTravelTime = travelTime;
					if(minTravelTime <= mTravelTimeTarget)
					{
						break;
					}
				}
			}
			
			switch (selectedTransportOffer.getOfferType()) 
			{
			case WALKING:
				logger.info("decideTransportMethod() I am walking to my destination.");
				
				// walk there
				mTransportModeUsed = TransportMode.WALKING;
				mCurrentState = STATE.TRAVELING_ON_FOOT;
				mOnFootTravelPath = mWalkingTravelPlan;
				mCurrentPathIndex = 0;
				break;
			case TAKE_BUS:
				logger.info("decideTransportMethod() I am taking the bus to my destination.");
				
				// take the bus
				mTransportModeUsed = TransportMode.TAKE_BUS;
				mCurrentState = STATE.TRAVELING_TO_BUS_STOP;
				handleBusTravelPlan(busOffer);
				break;
			case TAKE_TAXI:
				logger.info("decideTransportMethod() I am taking the taxi to my destination " + taxiOffer.getData());
				
				// take the taxi
				mTransportModeUsed = TransportMode.TAKE_TAXI;
				mCurrentState = STATE.WAITING_FOR_TAXI;
				confirmRequest(taxiOffer);
				mTaxiConfirmationRequests.remove(taxiOffer);
				break;
			default:
				assert(false) : "decideTransportMethod(): Unhandled transport offer: " + selectedTransportOffer.getOfferType();
				break;
			}
			
			if(mTaxiConfirmationRequests.size() > 0)
			{
				for (Iterator<RequestTaxiConfirmationMessage> iterator = mTaxiConfirmationRequests.iterator(); 
						iterator.hasNext();) 
				{
					cancelTaxiRequest(iterator.next());
				}
			}
		}
	}
	
	private RequestTaxiConfirmationMessage getBestTaxiOffer(Queue<RequestTaxiConfirmationMessage> confirmationRequests)
	{
		logger.info("getBestTaxiOffer() confirmationRequests.size() " + confirmationRequests.size());
		
		if(confirmationRequests.size() > 0)
		{
			// we'll select the offer closest taxi to this user 
			RequestTaxiConfirmationMessage confirmedRequest = null;
			double minDistance = Double.MAX_VALUE;
			for(RequestTaxiConfirmationMessage request : confirmationRequests)
			{
				double distance = request.getData().getTotalTravelTime();
				if(minDistance > distance)
				{
					confirmedRequest = request;
					minDistance = distance;
				}
			}
			logger.info("getBestTaxiOffer() confirmedRequest " + confirmedRequest);
			
			return confirmedRequest;
		}
		return null;
	}
	
	private BusTravelPlanMessage getBestBusPlan(Queue<BusTravelPlanMessage> busTravelPlans)
	{
		if(busTravelPlans.size() > 0)
		{
			BusTravelPlanMessage bestTravelPlanMessage = null;
			double minCost = Double.MAX_VALUE;
			for(BusTravelPlanMessage travelPlan : busTravelPlans)
			{
				int distanceToFirstBusStop = travelPlan.getData().getPathToFirstBusStop().size();
				int distanceToDestination = travelPlan.getData().getPathToDestination().size();
				int distanceByBus = travelPlan.getData().getBusTravelDistance();
				int totalCost = distanceToFirstBusStop + distanceToDestination + distanceByBus;
				if( minCost > totalCost)
				{
					minCost = totalCost;
					bestTravelPlanMessage = travelPlan;
				}
			}
			return bestTravelPlanMessage;
		}
		return null;
	}
	
	private void logTransportOffers(List<TransportOffer> transportOffers)
	{
		for (int i = 0; i < transportOffers.size(); ++i) 
		{
			TransportOffer offer = transportOffers.get(i);
			logger.info("decideTransportMethod() " + offer.getOfferType() + 
					" costs: " + offer.getCost() + " currency units");
			logger.info("decideTransportMethod() " + offer.getOfferType() + 
					" takes: " + offer.getTimeTaken() + " time units");
		}
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
			else if(input instanceof RequestTaxiConfirmationMessage)
			{
				cancelTaxiRequest((RequestTaxiConfirmationMessage)input);
			}
			else if(input instanceof RequestDestinationMessage)
			{
				mWithTaxi.handleRequestDestination((RequestDestinationMessage)input);
			}
			else if(input instanceof DestinationReachedMessage)
			{
				mWithTaxi.handleOnDestinationReached((DestinationReachedMessage)input);
			}
			else if(input instanceof NotificationOfArrivalAtBusStop)
			{
				handleBusArrival((NotificationOfArrivalAtBusStop)input);
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
	}
	
	private void confirmRequest(RequestTaxiConfirmationMessage requestConfirmationMessage)
	{
		logger.info("confirmRequest()");
		
		TaxiRequestConfirmationMessage confirmationMessage = new TaxiRequestConfirmationMessage("I confirm the request",
				network.getAddress(), requestConfirmationMessage.getFrom());
		network.sendMessage(confirmationMessage);
	}
	
	private void cancelTaxiRequest(RequestTaxiConfirmationMessage requestToCancel)
	{
		logger.info("cancelRequest() " + requestToCancel);
		
		TaxiRequestCancelMessage cancelMessage = new TaxiRequestCancelMessage("I cancel this request", 
				network.getAddress(), requestToCancel.getFrom());
		network.sendMessage(cancelMessage);
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
		
		mCurrentState = STATE.TRAVELING_BY_TAXI;
	}
	
	private void onDestinationReached()
	{		
		mCurrentState = STATE.REACHED_DESTINATION;
	}
	
	private void handleBusTravelPlan(BusTravelPlanMessage msg)
	{
		logger.info("handleBusTravelPlan() mCurrentState " + mCurrentState);
		
		mBusTravelPlan = msg.getData();
		mCurrentPathIndex = 0;
		
		logger.info("handleBusTravelPlan() mBusTravelPlan.getPathToFirstBusStop() " + mBusTravelPlan.getPathToFirstBusStop());
		logger.info("handleBusTravelPlan() mBusTravelPlan.getPathToDestination() " + mBusTravelPlan.getPathToDestination());
		
		moveTo(mBusTravelPlan.getPathToFirstBusStop().get(mCurrentPathIndex++));
	}
	
	private void handleBusArrival(NotificationOfArrivalAtBusStop notification)
	{		
		if(notification.getData().getBusRoute().getBusRouteID().
				equals(mBusTravelPlan.getBusRouteID()))
		{
			
			switch (mCurrentState) 
			{
			case IN_BUS_STOP:
			{
				logger.info("handleBusArrival() The bus has arrived " + notification.getFrom());
				
				mCurrentState = STATE.WAITING_BUS_BOARD_CONFIRMATION;
				
				// send a request to board the bus
				BoardBusRequest request = new BoardBusRequest(authkey);
				BoardBusRequestMessage msg = new BoardBusRequestMessage(request, 
						network.getAddress(), notification.getFrom());
				network.sendMessage(msg);	
			}
			break;
			case TRAVELING_BY_BUS:
			{
				Location targetBusStop = mBusTravelPlan.getPathToDestination().get(0);
				Location currentBusStop = notification.getData().getBusStopLocation();
				if(currentBusStop.equals(targetBusStop))
				{
					logger.info("handleBusArrival() I've reached my bus stop " + targetBusStop);
					
					mCurrentState = STATE.WAITING_BUS_UNBOARD_CONFIRMATION;
					// send a request to unboard the bus
					String request = "This is my stop!";
					UnBoardBusRequestMessage msg = new UnBoardBusRequestMessage(request, 
							network.getAddress(), notification.getFrom());
					network.sendMessage(msg);
				}
			}
			default:
				break;
			}
		}
	}
	
	private void handleBusBoardSuccesful(BusBoardingSuccessfulMessage msg)
	{
		logger.info("handleBusBoardSuccesful() I've boarded the bus " + msg.getFrom());
		
		assert (mCurrentState == STATE.WAITING_BUS_BOARD_CONFIRMATION);
		
		mCurrentState = STATE.TRAVELING_BY_BUS;
	}
	
	private void handleBusBoardFailure(BusIsFullMessage msg)
	{
		logger.info("handleBusBoardFailure() This bus is full " + msg.getFrom());
		
		assert (mCurrentState == STATE.WAITING_BUS_BOARD_CONFIRMATION);
		
		mCurrentState = STATE.IN_BUS_STOP;
	}
	
	private void handleBusUnBoarded(BusUnBoardingSuccessful msg)
	{
		logger.info("handleBusUnBoarded() I've got of the bus " + msg.getFrom());
		
		assert (mCurrentState == STATE.WAITING_BUS_UNBOARD_CONFIRMATION);
		
		mCurrentState = STATE.TRAVELING_ON_FOOT;
		mCurrentPathIndex = 0;
		mOnFootTravelPath = mBusTravelPlan.getPathToDestination();
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
			
			mUserDataStore.setHasReachedDestination(false);
		}
		else 
		{
			mUserDataStore.setHasReachedDestination(true);
		}
		mUserDataStore.setPathTraveled(mPathTraveled);
		mUserDataStore.setTransportPreference(mTransportPreference);
		mUserDataStore.setTransportMethodUsed(mTransportModeUsed);
		mUserDataStore.setTravelTimeTarget(mTravelTimeTarget);
		mUserDataStore.setTravelTime(Math.round(mTravelTime));
		
		mSimulationDataStore.addUserDataStore(getID(), mUserDataStore);
	}

	@Override
	public double getPerceptionRange() 
	{
		return 1;
	}
}
