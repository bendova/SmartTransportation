package agents;

import java.util.*;

import map.CityMap;

import com.google.inject.Inject;

import conversations.busStationBus.BusRouteMessage;
import conversations.busStationBus.RegisterAsBusMessage;
import conversations.busStationBus.messageData.BusRoute;
import conversations.busStationMediator.RegisterAsBusServiceMessage;
import conversations.userBusStation.BusTravelPlan;
import conversations.userBusStation.BusTravelPlanMessage;
import conversations.userMediator.messages.TransportServiceRequestMessage;

import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class BusStation extends AbstractParticipant
{
	private Location mLocation;
	private NetworkAddress mMediatorAddress;
	private Map<NetworkAddress, UUID> mBusesMap;
	private List<NetworkAddress> mFreeBuses;
	private List<Location> mBusStops;
	private ParticipantLocationService mLocationService;
	private UUID mBusRouteID;
	
	@Inject
	private CityMap mCityMap;
	
	public BusStation(UUID id, String name, Location location, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mMediatorAddress = mediatorNetworkAddress;
		
		mBusesMap = new HashMap<NetworkAddress, UUID>();
		mFreeBuses = new LinkedList<NetworkAddress>();
	}
	
	public NetworkAddress getNetworkAddress() 
	{
		return network.getAddress();
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return ss;
	}
	
	public void setBusRoute(List<Location> busStops)
	{
		assert(busStops != null) : "BusStation::setBusRoute() busStops is null!";
		assert(busStops.isEmpty() == false) : "BusStation::setBusRoute() busStops list is empty!";
		
		mBusStops = busStops;
		mBusRouteID = Random.randomUUID();
	}
	
	@Override
	public void initialise()
	{
		super.initialise();
		
		registerAsBusServiceProvider();
		initializeLocationService();
		//initializeProtocols();
	}
	
	private void registerAsBusServiceProvider()
	{
		RegisterAsBusServiceMessage msg = new RegisterAsBusServiceMessage(
				network.getAddress(), mMediatorAddress);
		network.sendMessage(msg);
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
	
	@Override
	protected void processInput(Input input) 
	{
		logger.info("processInput() " + input);
		
		assert (input != null) : "BusStation::processInput() input is null!";
		
		if(input instanceof TransportServiceRequestMessage)
		{
			replyToRequest((TransportServiceRequestMessage)input);
		}
		else if(input instanceof RegisterAsBusMessage)
		{
			registerBus((RegisterAsBusMessage)input);
		}
	}
	
	private void replyToRequest(TransportServiceRequestMessage msg)
	{
		logger.info("replyToRequest() " + msg.getData().getMessage());
		
		// find nearest bus station to start location
		Location start = msg.getData().getStartLocation();
		Location startBusStop = getNearestBusStopTo(start);
		List<Location> startTravelPath = getPath(start, startBusStop);
		
		// find nearest bus station to destination
		Location destination = msg.getData().getDestination();
		Location targetBusStop = getNearestBusStopTo(destination);
		List<Location> destinationTravelPath = getPath(destination, targetBusStop); 
		Collections.reverse(destinationTravelPath);
		
		// TODO find the buses that follows that route		
		// TODO get those buses' arrival times 
		
		// reply to the user with the travel paths
		BusTravelPlan travelPlan = new BusTravelPlan(startTravelPath, destinationTravelPath, mBusRouteID);
		BusTravelPlanMessage travelPlanMsg = new BusTravelPlanMessage(travelPlan, 
				network.getAddress(), msg.getFrom());
		network.sendMessage(travelPlanMsg);
	}
	
	private void registerBus(RegisterAsBusMessage msg)
	{
		NetworkAddress busAddress = msg.getFrom();
		mBusesMap.put(busAddress, msg.getData());
		mFreeBuses.add(busAddress);
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		dispatchBuses();
	}
	
	private void dispatchBuses()
	{
		if(mFreeBuses.isEmpty() == false)
		{
			for (Iterator<NetworkAddress> iterator = mFreeBuses.iterator(); 
					iterator.hasNext();) 
			{
				sendRouteToBus(iterator.next());
				Collections.reverse(mBusStops);
				iterator.remove();
			}
		}
	}
	
	private void sendRouteToBus(NetworkAddress busAddress)
	{
		BusRoute busRoute = new BusRoute(new ArrayList<Location>(mBusStops), mBusRouteID);
		BusRouteMessage routeMsg = new BusRouteMessage(network.getAddress(), 
				busAddress, busRoute); 
		network.sendMessage(routeMsg);
	}
	
	private Location getNearestBusStopTo(Location location)
	{
		Location targetBusStop = null;
		double minDistance = Double.MAX_VALUE;
		for (Iterator<Location> iterator = mBusStops.iterator(); iterator.hasNext();) 
		{
			Location busStop = iterator.next();
			if(busStop.distanceTo(location) < minDistance)
			{
				targetBusStop = busStop;
				minDistance = busStop.distanceTo(location);
			}
		}
		return targetBusStop;
	}
	
	public List<Location> getPath(Location start, Location destination)
	{
		Set<Location> evaluatedLocations = new HashSet<Location>();
		Map<Location, Location> traveledPaths = new HashMap<Location, Location>();
		Set<Location> locationsToEvaluate = new HashSet<Location>();
		locationsToEvaluate.add(start);
		
		Map<Location, Integer> globalCostMap = new HashMap<Location, Integer>();
		globalCostMap.put(start, 0);
		Map<Location, Integer> estimatedCostMap = new HashMap<Location, Integer>();
		estimatedCostMap.put(start, Integer.valueOf((int)start.distanceTo(destination)));
		
		while(locationsToEvaluate.isEmpty() == false)
		{
			Location currentLocation = null;
			int minCost = Integer.MAX_VALUE;
			for (Iterator<Location> iterator = locationsToEvaluate.iterator(); iterator.hasNext();)
			{
				Location location = iterator.next();
				if(estimatedCostMap.get(location) < minCost)
				{
					minCost = estimatedCostMap.get(location);
					currentLocation = location;
				}
			}
			
			if(currentLocation.equals(destination))
			{
				return reconstructPath(traveledPaths, currentLocation);
			}
			
			// check if our destination is still the best one
			Location bestBusStop = getNearestBusStopTo(currentLocation);
			if(bestBusStop.equals(destination) == false)
			{
				// change of plans
				for (Iterator<Location> iterator = locationsToEvaluate.iterator(); iterator.hasNext();)
				{
					Location location = iterator.next();
					int currentCost = estimatedCostMap.get(location);
					int newCost = (currentCost - (int)location.distanceTo(destination)) 
							+ (int)location.distanceTo(bestBusStop);
					estimatedCostMap.put(location, newCost);
				}
				
				// update our destination
				destination = bestBusStop;
			}
			
			locationsToEvaluate.remove(currentLocation);
			evaluatedLocations.add(currentLocation);
			// evaluate the neighbors
			List<Location> neighborsList = getNeighboringLocations(currentLocation);
			for (Iterator<Location> iterator2 = neighborsList.iterator(); 
					iterator2.hasNext();) 
			{
				Location neighbor = iterator2.next();
				int cost = globalCostMap.get(currentLocation) + (int)currentLocation.distanceTo(neighbor);
				int estimatedTotalCost = cost + (int)neighbor.distanceTo(destination);
				
				if(evaluatedLocations.contains(neighbor) && (estimatedTotalCost >= estimatedCostMap.get(neighbor)))
				{
					continue;
				}
				
				if((locationsToEvaluate.contains(neighbor) == false) || (estimatedTotalCost < estimatedCostMap.get(neighbor)))
				{
					traveledPaths.put(neighbor, currentLocation);
					globalCostMap.put(neighbor, cost);
					estimatedCostMap.put(neighbor, estimatedTotalCost);
					if(locationsToEvaluate.contains(neighbor) == false)
					{
						locationsToEvaluate.add(neighbor);
					}
				}
			}
		}
		return new ArrayList<Location>();
	}
	
	private List<Location> getNeighboringLocations(Location location)
	{
		List<Location> neighbors = new ArrayList<Location>();
		
		int x = (int) location.getX(); 
		int y = (int) location.getY(); 
		if((mCityMap.isValidLocation(--x, y)))
		{
			neighbors.add(new Location(x, y));
		}
		++x;
		if(mCityMap.isValidLocation(++x, y))
		{
			neighbors.add(new Location(x, y));
		}
		--x;
		if((mCityMap.isValidLocation(x, --y)))
		{
			neighbors.add(new Location(x, y));
		}
		++y;
		if(mCityMap.isValidLocation(x, ++y))
		{
			neighbors.add(new Location(x, y));
		}
		--y;

		return neighbors;
	}
	
	private List<Location> reconstructPath(Map<Location, Location> traveledPaths, 
			Location currentLocation)
	{
		if(traveledPaths.containsKey(currentLocation))
		{
			List<Location> path = reconstructPath(traveledPaths, traveledPaths.get(currentLocation));
			path.add(currentLocation);
			return path;
		}
		else 
		{
			List<Location> path = new ArrayList<Location>();
			path.add(currentLocation);
			return path;
		}
	}
	
}
