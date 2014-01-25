package agents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
import conversations.userBus.messages.NotificationOfArrivalAtBusStop;
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
	enum STATE
	{
		IDLE,
		TRAVELING_TO_FIRST_BUS_STOP,
		FOLLOWING_ROUTE
	}
	
	private STATE mCurrentState;
	private Location mCurrentLocation;
	private NetworkAddress mBusStationAddress;
	private ParticipantLocationService mLocationService;
	private List<Location> mBusStops;
	private List<Location> mPathToTravel;
	private int mCurrentPathIndex;
	private ArrayList<Location> mTraveledLocations; 
	private IBusRoute mBusRoute;
	
	@Inject
	private CityMap mCityMap;
	
	public Bus(UUID id, String name, Location location, NetworkAddress busStationAddress) 
	{
		super(id, name);
		mCurrentLocation = location;
		mBusStationAddress = busStationAddress;
		
		mCurrentState = STATE.IDLE;
		mTraveledLocations = new ArrayList<Location>();
		mPathToTravel = new ArrayList<Location>();
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
//			planPathToTravel();
		}
	}
	
	private void travelToFistBusStop()
	{
		// how to get to the first bus stop
		mPathToTravel = mCityMap.getPath(mCurrentLocation, mBusStops.get(0));
		mCurrentState = STATE.TRAVELING_TO_FIRST_BUS_STOP;
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
		}
		catch (ActionHandlingException e) 
		{
			logger.warn("Error while moving!", e);
		}
	}
	
	private boolean isBusStop(Location location)
	{
		for (Location busStop : mPathToTravel) 
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
				mCurrentState = STATE.FOLLOWING_ROUTE;
				mCurrentPathIndex = 0;
			}
			break;
		case FOLLOWING_ROUTE:
			if(isBusStop(mCurrentLocation))
			{
				// TODO 
				// notify users in bus that a bus station is reached
				// notify users in bus station to board the bus
				// wait 1 time step for them to unboard / board
				// travel to next bus station
				
				logger.info("incrementTime() I'm at bus stop " + mCurrentLocation);
				
				Map<UUID, Location> users =  mLocationService.getNearbyAgents();
				Iterator<Entry<UUID, Location>> iterator = users.entrySet().iterator();
				while ( iterator.hasNext() ) 
				{
					Entry<UUID, Location> user = iterator.next();
					
					logger.info("incrementTime() I can see here " + user.getKey());
					
					notifyUserOfArrivalInBusStop(user.getKey());
				}
			}
			
			if(mCurrentPathIndex < mPathToTravel.size())
			{
				moveTo(mPathToTravel.get(mCurrentPathIndex++));
			}
			else
			{
				mCurrentPathIndex = 0;
			}
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
	
	private void notifyUserOfArrivalInBusStop(UUID userUuid)
	{
		NetworkAddress from = new NetworkAddress(getID());
		logger.info("notifyUserOfArrivalInBusStop() (from.equals(network.getAddress()) " + 
					(from.equals(network.getAddress())));
		
		BusStopArrivalNotification notification = new BusStopArrivalNotification(mCurrentLocation, mBusRoute);
		NetworkAddress to = new NetworkAddress(userUuid);
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
		return 1;
	}
}
