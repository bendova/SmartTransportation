package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import SmartTransportation.Simulation;

import com.google.inject.Inject;

import map.CityMap;

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

public class Bus extends AbstractParticipant implements HasPerceptionRange
{
	enum State
	{
		IDLE,
		TRAVELING_TO_FIRST_BUS_STOP,
		FOLLOWING_ROUTE,
		AT_BUS_STOP,
		RETURNING_TO_ROUTE
	}
	
	private State mCurrentState;
	private Location mCurrentLocation;
	private NetworkAddress mBusStationAddress;
	private ParticipantLocationService mLocationService;
	private List<Location> mBusStops;
	private List<Location> mPathToTravel;
	private int mCurrentPathIndex;
	private ArrayList<Location> mTraveledLocations; 
	private IBusRoute mBusRoute;
	
	private final static int MAX_PASSANGERS_COUNT = 2;
	
	// mapping of the passangers'
	// network address(so we can talk to them) to their
	// enviroment authentication key (so we can transport them)
	private Map<NetworkAddress, UUID> mPassangers; 
	
	private final static int MAX_BUS_STOP_WAIT_TIME = 2;
	private int mBusStopWaitTime;
	
	@Inject
	private CityMap mCityMap;
	
	public Bus(UUID id, String name, Location location, NetworkAddress busStationAddress) 
	{
		super(id, name);
		mCurrentLocation = location;
		mBusStationAddress = busStationAddress;
		
		mCurrentState = State.IDLE;
		mTraveledLocations = new ArrayList<Location>();
		mPathToTravel = new ArrayList<Location>();
		mPassangers = new HashMap<NetworkAddress, UUID>();
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
		assert(routeMessage.getData().getBusStops().isEmpty() == false) : 
			"Bus::processMessage() The bus stops list is empty!";
		
		if(routeMessage.getFrom().equals(mBusStationAddress))
		{
			mBusRoute = routeMessage.getData();
			mBusStops = mBusRoute.getBusStops();
			travelToFistBusStop();
		}
	}
	
	private void processMessage(BoardBusRequestMessage boardRequestMessage)
	{
		logger.info("processMessage() boardRequestMessage from " + boardRequestMessage.getFrom());
		
		if(mPassangers.size() < MAX_PASSANGERS_COUNT)
		{
			mPassangers.put(boardRequestMessage.getFrom(), 
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
	
	private void processMessage(UnBoardBusRequestMessage unBoardRequest)
	{
		logger.info("processMessage() unBoardRequestMessage from " + unBoardRequest.getFrom());
		
		if(mCurrentState == State.AT_BUS_STOP)
		{
			NetworkAddress address = unBoardRequest.getFrom();
			if(mPassangers.containsKey(unBoardRequest.getFrom()))
			{
				mPassangers.remove(address);
				String reply = "You have successfully unboarded the bus.";
				BusUnBoardingSuccessful msg = new BusUnBoardingSuccessful(reply, 
						network.getAddress(), address);
				network.sendMessage(msg);
			}
		}
		else 
		{
			assert (false) : "Received unboard request while not in bus stop from " + unBoardRequest.getFrom();
		}
	}
	
	private void travelToFistBusStop()
	{
		// how to get to the first bus stop
		mPathToTravel = mCityMap.getPath(mCurrentLocation, mBusStops.get(0));
		mCurrentState = State.TRAVELING_TO_FIRST_BUS_STOP;
		mCurrentPathIndex = 0;
		moveTo(mPathToTravel.get(mCurrentPathIndex++));
	}
	
	private void planPathToTravel()
	{
		mPathToTravel.clear();
		// how to follow the route
		int busStopsCount = mBusStops.size();
		Location currentBusStop = mBusStops.get(0);
		for(int i = 1; i < busStopsCount; ++i)
		{
			Location nextBusStopLocation = mBusStops.get(i);
			mPathToTravel.addAll(mCityMap.getPath(currentBusStop, 
					nextBusStopLocation));
			currentBusStop = nextBusStopLocation;
		}
		
		// the route is circular
		Location nextBusStopLocation = mBusStops.get(0);
		mPathToTravel.addAll(mCityMap.getPath(currentBusStop, 
				nextBusStopLocation));
	}
	
	private void moveTo(Location targetLocation)
	{
		Move move = new Move(mCurrentLocation.getMoveTo(targetLocation));
		try 
		{
			environment.act(move, getID(), authkey);
			
			if(mPassangers.size() > 0)
			{
				Iterator<Entry<NetworkAddress, UUID>> iterator = 
						mPassangers.entrySet().iterator();
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
		List<Location> busStops = mBusRoute.getBusStops();
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
			else 
			{
				planPathToTravel();
				mCurrentState = State.FOLLOWING_ROUTE;
				mCurrentPathIndex = 0;
			}
			break;
		case FOLLOWING_ROUTE:
			if(isBusStop(mCurrentLocation))
			{
				logger.info("incrementTime() I'm at bus stop " + mCurrentLocation);
				
				mCurrentState = State.AT_BUS_STOP;
				mBusStopWaitTime = 0;
				
				logger.info("incrementTime() I have  " + mPassangers.size() + " passengers.");
				
				// notify users in bus that a bus station is reached
				if(mPassangers.size() > 0)
				{
					Iterator<Entry<NetworkAddress, UUID>> iterator = mPassangers.entrySet().iterator();
					while ( iterator.hasNext() ) 
					{
						Entry<NetworkAddress, UUID> passenger = iterator.next();
						
						notifyUserOfArrivalInBusStop(passenger.getKey());
					}
				}
				
				// notify users in bus station to board the bus
				Map<UUID, Location> users =  mLocationService.getNearbyAgents();
				
				logger.info("incrementTime() There are  " + users.size() 
						+ " agents in the bus stop.");
				
				Iterator<Entry<UUID, Location>> iterator = users.entrySet().iterator();
				while ( iterator.hasNext() ) 
				{
					Entry<UUID, Location> user = iterator.next();
					
					logger.info("incrementTime() I can see here " + user.getKey());
					
					NetworkAddress to = new NetworkAddress(user.getKey());
					notifyUserOfArrivalInBusStop(to);
				}
				
			}
			else 
			{
				mCurrentState = State.FOLLOWING_ROUTE;
				if(mCurrentPathIndex < mPathToTravel.size())
				{
					moveTo(mPathToTravel.get(mCurrentPathIndex++));
				}
				else
				{
					mCurrentPathIndex = 0;
				}
			}
			break;
		case AT_BUS_STOP:
			if(mBusStopWaitTime == MAX_BUS_STOP_WAIT_TIME)
			{
				mCurrentState = State.RETURNING_TO_ROUTE;
			}
			else 
			{
				++mBusStopWaitTime;
			}
			break;
		case RETURNING_TO_ROUTE:
			mCurrentState = State.FOLLOWING_ROUTE;
			if(mCurrentPathIndex < mPathToTravel.size())
			{
				moveTo(mPathToTravel.get(mCurrentPathIndex++));
			}
			else
			{
				mCurrentPathIndex = 0;
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
	
	private void notifyUserOfArrivalInBusStop(NetworkAddress to)
	{
		BusStopArrivalNotification notification = new BusStopArrivalNotification(mCurrentLocation, mBusRoute);
		NotificationOfArrivalAtBusStop msg = new NotificationOfArrivalAtBusStop(notification, 
				network.getAddress(), to);
		network.sendMessage(msg);
	}
	
	@Override
	public void onSimulationComplete()
	{
		Simulation.addBusLocations(getName(), mTraveledLocations);
	}

	@Override
	public double getPerceptionRange() 
	{
		return 0;
	}
}
