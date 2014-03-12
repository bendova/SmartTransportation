package agents;

import gui.AgentDataForMap.AgentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import map.CityMap;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.Move;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;
import uk.ac.imperial.presage2.util.participant.HasPerceptionRange;
import util.movement.TransportMove;
import SmartTransportation.Simulation.TransportMethodSpeed;

import conversations.busStationBus.BusRouteMessage;
import conversations.busStationBus.RegisterAsBusMessage;
import conversations.busStationBus.messageData.IBusRoute;
import conversations.userBus.messages.BoardBusRequestMessage;
import conversations.userBus.messages.BusBoardingSuccessfulMessage;
import conversations.userBus.messages.BusIsFullMessage;
import conversations.userBus.messages.BusUnBoardingSuccessful;
import conversations.userBus.messages.NotificationOfArrivalAtBusStop;
import conversations.userBus.messages.UnBoardBusRequestMessage;
import conversations.userBus.messages.messageData.BusStopArrivalNotification;
import dataStores.AgentDataStore;
import dataStores.AgentMoveData;
import dataStores.MoveData;
import dataStores.SimulationDataStore;

public class Bus extends AbstractParticipant implements HasPerceptionRange
{
	enum State
	{
		IDLE,
		TRAVELING_TO_FIRST_BUS_STOP,
		FOLLOWING_ROUTE,
		AT_BUS_STOP,
		PREPARE_TO_RETURN_TO_ROUTE
	}
	
	private State mCurrentState;
	private Location mStartLocation;
	private Location mCurrentLocation;
	private Location mNextBusStop;
	private int mNextBusIndex;
	private NetworkAddress mBusStationAddress;
	private ParticipantLocationService mLocationService;
	private List<Location> mBusStops;
	private List<Location> mPathToTravel;
	private int mCurrentPathIndex;
	private IBusRoute mBusRoute;
	
	private final static int MAX_PASSANGERS_COUNT = 5;
	
	// mapping of the passengers'
	// network addresses(so we can talk to them) to their
	// environment authentication keys (so we can transport them)
	private Map<NetworkAddress, UUID> mPassengers; 
	
	private static int MAX_BUS_STOP_WAIT_TIME;
	private int mBusStopWaitTime;
	
	private List<BoardBusRequestMessage> mBoardRequests;
	private List<UnBoardBusRequestMessage> mUnBoardRequests;
	
	private CityMap mCityMap;
	private SimulationDataStore mSimulationDataStore;
	
	private int mTimeTakenPerUnitDistance;
	
	public Bus(UUID id, String name, CityMap cityMap, Location location, NetworkAddress busStationAddress) 
	{		
		super(id, name);
		
		assert(cityMap != null);
		assert(location != null);
		assert(busStationAddress != null);
		
		mCityMap = cityMap;
		mStartLocation = location;
		mCurrentLocation = location;
		mBusStationAddress = busStationAddress;
		
		mCurrentState = State.IDLE;
		mPathToTravel = new ArrayList<Location>();
		mPassengers = new HashMap<NetworkAddress, UUID>();
		
		mBoardRequests = new LinkedList<BoardBusRequestMessage>();
		mUnBoardRequests = new LinkedList<UnBoardBusRequestMessage>();
		
		mTimeTakenPerUnitDistance = TransportMethodSpeed.BUS_SPEED.getTimeTakenPerUnitDistance();
		MAX_BUS_STOP_WAIT_TIME = 2 * mTimeTakenPerUnitDistance;
	}
	
	public void setDataStore(SimulationDataStore dataStore) 
	{
		assert(dataStore != null);
		mSimulationDataStore = dataStore;
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mCurrentLocation));
		return ss;
	}
	
	@Override
	public void initialise() 
	{
		super.initialise();
		
		initializeLocationService();
//		initialiseProtocols();
		registerToBusService();
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
	
	private void registerToBusService()
	{
		RegisterAsBusMessage msg = new RegisterAsBusMessage(getID(), 
				network.getAddress(), mBusStationAddress);
		network.sendMessage(msg);
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input instanceof BusRouteMessage)
		{
			processMessage((BusRouteMessage)input);
		}
		else if(input instanceof BoardBusRequestMessage)
		{
			processMessage((BoardBusRequestMessage)input);
		}
		else if(input instanceof UnBoardBusRequestMessage)
		{
			processMessage((UnBoardBusRequestMessage)input);
		}
	}
	
	private void processMessage(BusRouteMessage routeMessage)
	{
		assert(routeMessage.getData().getBusStopsLocations().isEmpty() == false) : 
			"Bus::processMessage() The bus stops list is empty!";
		
		if(routeMessage.getFrom().equals(mBusStationAddress))
		{
			mBusRoute = routeMessage.getData();
			mBusStops = mBusRoute.getBusStopsLocations();
			travelToFistBusStop();
		}
		else 
		{
			assert(false) : "Bus::processMessage() Received BusRouteMessage from someone other than " +
					"our bus station: " + routeMessage.getFrom();
		}
	}
	
	private void travelToFistBusStop()
	{
		mCurrentState = State.TRAVELING_TO_FIRST_BUS_STOP;
		
		mNextBusIndex = 0;
		mNextBusStop = mBusStops.get(mNextBusIndex);
		mPathToTravel = mCityMap.getPath(mCurrentLocation, mNextBusStop);
		mCurrentPathIndex = 0;
		moveTo(mPathToTravel.get(mCurrentPathIndex++));
	}
	
	private void processMessage(BoardBusRequestMessage boardRequestMessage)
	{
		logger.info("processMessage() boardRequestMessage from " + boardRequestMessage.getFrom());
		
		mBoardRequests.add(boardRequestMessage);
	}
	
	private void processMessage(UnBoardBusRequestMessage unBoardRequest)
	{
		logger.info("processMessage() unBoardRequestMessage from " + unBoardRequest.getFrom());
		
		mUnBoardRequests.add(unBoardRequest);
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		updateLocation();
		switch (mCurrentState) 
		{
		case TRAVELING_TO_FIRST_BUS_STOP:
			if(mCurrentPathIndex < mPathToTravel.size())
			{
				moveTo(mPathToTravel.get(mCurrentPathIndex++));
			}
			else if(mCurrentLocation.equals(mNextBusStop))
			{
				notifyOfBusStopArrival();
				mCurrentState = State.AT_BUS_STOP;
			}
			break;
		case FOLLOWING_ROUTE:
			if(mCurrentLocation.equals(mNextBusStop))
			{				
				notifyOfBusStopArrival();
				mCurrentState = State.AT_BUS_STOP;
			}
			else 
			{
				followRoute();
			}
			break;
		case AT_BUS_STOP:
			handleUnBoardRequests();
			
			++mBusStopWaitTime;
			if(mBusStopWaitTime >= MAX_BUS_STOP_WAIT_TIME)
			{
				mBusStopWaitTime = 0;				
				mCurrentState = State.PREPARE_TO_RETURN_TO_ROUTE;
			}
			break;
		case PREPARE_TO_RETURN_TO_ROUTE:
			handleBoardRequests();
				
			planRouteToNextBusStop();
			followRoute();
			mCurrentState = State.FOLLOWING_ROUTE;
			break;
		}
	}
	
	private void planRouteToNextBusStop()
	{
		mCurrentPathIndex = 0;
		mPathToTravel = mBusRoute.getPathsBetweenBusStops().get(mNextBusStop);
		
		if(mNextBusIndex < mBusStops.size())
		{
			++mNextBusIndex;
		}
		else 
		{
			mNextBusIndex = 0;
		}
		mNextBusStop = mBusStops.get(mNextBusIndex);
	}
	
	private void followRoute()
	{
		if(mCurrentPathIndex < mPathToTravel.size())
		{
			moveTo(mPathToTravel.get(mCurrentPathIndex++));
		}
	}
	
	private void notifyOfBusStopArrival()
	{
		logger.info("notifyOfBusStopArrival() I'm at bus stop " + mCurrentLocation);
		
		// notify users in bus station and passengers 
		// that a bus stop has been reached
		Map<UUID, Location> users = mLocationService.getNearbyAgents();
		
		logger.info("notifyOfBusStopArrival() I have " + mPassengers.size() + " passengers.");
		logger.info("notifyOfBusStopArrival() There are " + users.size() 
				+ " agents in the bus stop.");
		
		Iterator<Entry<UUID, Location>> iterator = users.entrySet().iterator();
		while ( iterator.hasNext() ) 
		{
			Entry<UUID, Location> user = iterator.next();
			
			logger.info("notifyOfBusStopArrival() I can see here " + user.getKey());
			
			NetworkAddress to = new NetworkAddress(user.getKey());
			notifyUserOfArrivalInBusStop(to);
		}
	}
	
	private void notifyUserOfArrivalInBusStop(NetworkAddress to)
	{
		BusStopArrivalNotification notification = new BusStopArrivalNotification(mCurrentLocation, mBusRoute);
		NotificationOfArrivalAtBusStop msg = new NotificationOfArrivalAtBusStop(notification, 
				network.getAddress(), to);
		network.sendMessage(msg);
	}
	
	private void handleUnBoardRequests()
	{
		logger.info("handleUnBoardRequests()  mUnBoardRequests.size() " + mUnBoardRequests.size());
		
		Iterator<UnBoardBusRequestMessage> iterator = mUnBoardRequests.iterator();
		while(iterator.hasNext())
		{
			UnBoardBusRequestMessage unBoardRequest = iterator.next();
			NetworkAddress address = unBoardRequest.getFrom();
			if(mPassengers.containsKey(address))
			{
				mPassengers.remove(address);
				String reply = "You have successfully unboarded the bus.";
				BusUnBoardingSuccessful msg = new BusUnBoardingSuccessful(reply, 
						network.getAddress(), address);
				network.sendMessage(msg);
			}
			else 
			{
				assert(false): "Received unboard request from a non-passenger: " + address;
			}
		}
		mUnBoardRequests.clear();
		
		logger.info("handleUnBoardRequests()  Passengers remaining after unboarding: " + mPassengers.size());
	}
	
	private void handleBoardRequests()
	{
		logger.info("handleBoardRequests()  mBoardRequests.size() " + mBoardRequests.size());
		
		Iterator<BoardBusRequestMessage> iterator = mBoardRequests.iterator();
		while(iterator.hasNext())
		{
			BoardBusRequestMessage boardRequestMessage = iterator.next();
			if(mPassengers.size() < MAX_PASSANGERS_COUNT)
			{
				mPassengers.put(boardRequestMessage.getFrom(), 
						boardRequestMessage.getData().getUserAuthKey());
				
				// send a reply informing that the user has boarded the bus
				String reply = "You have boarded the bus!";
				BusBoardingSuccessfulMessage msg = new BusBoardingSuccessfulMessage(reply, 
						network.getAddress(), boardRequestMessage.getFrom());
				network.sendMessage(msg);
			}
			else 
			{
				// send a reply informing that the bus is full
				String reply = "Sorry, there is no more room on the bus!";
				BusIsFullMessage msg = new BusIsFullMessage(reply, network.getAddress(),
						boardRequestMessage.getFrom());
				network.sendMessage(msg);
			}
		}
		mBoardRequests.clear();
		
		logger.info("handleBoardRequests()  Passengers after boarding: " + mPassengers.size());
	}
	
	private void updateLocation()
	{	
		Location currentLocation = mLocationService.getAgentLocation(getID());
		if(mCurrentLocation.equals(currentLocation) == false)
		{
			logger.info("updateLocation() currentLocation " + currentLocation);
			
			mCurrentLocation = currentLocation;
		}
	}
	
	private void moveTo(Location targetLocation)
	{
		TransportMove move = new TransportMove(targetLocation, 
				mTimeTakenPerUnitDistance);
		try 
		{
			environment.act(move, getID(), authkey);
			
			if(mPassengers.size() > 0)
			{
				Iterator<Entry<NetworkAddress, UUID>> iterator = 
						mPassengers.entrySet().iterator();
				while ( iterator.hasNext() ) 
				{
					Entry<NetworkAddress, UUID> passenger = iterator.next();
					environment.act(move, passenger.getKey().getId(), passenger.getValue());
				}
			}
		}
		catch (ActionHandlingException e) 
		{
			logger.warn("Error while moving!", e);
		}
	}
	
	private boolean isBusStop(Location location)
	{
		List<Location> busStops = mBusRoute.getBusStopsLocations();
		for (Location busStop : busStops) 
		{
			if(busStop.equals(location))
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onSimulationComplete()
	{
		AgentDataStore dataStore = new AgentDataStore(getName(), getID(), 
				AgentType.BUS, mStartLocation);
		mSimulationDataStore.addAgentDataStore(getID(), dataStore);
	}

	@Override
	public double getPerceptionRange() 
	{
		return 0;
	}
}
