package agents;

import java.util.*;

import map.CityMap;

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
	
	private List<List<Location>> mBusRoutesList;
	private Map<UUID, BusRoute> mBusRoutes;
	
	private ParticipantLocationService mLocationService;
	
	private CityMap mCityMap;
	
	public BusStation(UUID id, String name, CityMap cityMap, Location location, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		
		assert(cityMap != null);
		assert(location != null);
		assert(mediatorNetworkAddress != null);
		
		mCityMap = cityMap;
		mLocation = location;
		mMediatorAddress = mediatorNetworkAddress;
		
		mBusesMap = new HashMap<NetworkAddress, UUID>();
		mFreeBuses = new LinkedList<NetworkAddress>();
		
		mBusRoutesList = new ArrayList<List<Location>>();
		mBusRoutes = new HashMap<UUID, BusRoute>();
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
	
	public void addBusRoute(List<Location> busStops)
	{
		assert(busStops != null) : "BusStation::setBusRoute() busStops is null!";
		assert(busStops.isEmpty() == false) : "BusStation::setBusRoute() busStops list is empty!";
		
		mBusRoutesList.add(busStops);
	}
	
	@Override
	public void initialise()
	{
		super.initialise();
		
		registerAsBusServiceProvider();
		initializeLocationService();
		//initializeProtocols();
		initBusRoutes();
	}
	
	private void initBusRoutes()
	{
		Iterator<List<Location>> iterator = mBusRoutesList.iterator();
		while(iterator.hasNext())
		{
			UUID busRouteID = Random.randomUUID();
			
			HashMap<Location, List<Location>> pathsBetweenBusStops = new HashMap<Location, List<Location>>();
			
			List<Location> busStopsList = iterator.next();
			int busStopsCount = busStopsList.size();
			Location currentBusStop = busStopsList.get(0);
			List<Location> pathList;
			for(int i = 1; i < busStopsCount; ++i)
			{	
				Location nextBusStopLocation = busStopsList.get(i);
				pathList = mCityMap.getPath(currentBusStop, nextBusStopLocation);
				pathsBetweenBusStops.put(currentBusStop, pathList);
				currentBusStop = nextBusStopLocation;
			}
			
			// the route is circular
			Location nextBusStopLocation = busStopsList.get(0);
			pathList = mCityMap.getPath(currentBusStop, nextBusStopLocation);
			pathsBetweenBusStops.put(currentBusStop, pathList);
			
			BusRoute busRoute = new BusRoute(busRouteID, busStopsList, pathsBetweenBusStops);
			mBusRoutes.put(busRouteID, busRoute);
		}
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
		
		if(mBusesMap.isEmpty())
		{
			return;
		}
		
		List<Location> selectedStartTravelPath = null;
		List<Location> selectedDestinationTravelPath = null;
		UUID selectedBusRouteID = null;
		int minBusTravelDistance = Integer.MAX_VALUE;
		
		Iterator<Map.Entry<UUID, BusRoute>> busRouteIterator = mBusRoutes.entrySet().iterator();
		while(busRouteIterator.hasNext())
		{
			BusRoute busRoute = busRouteIterator.next().getValue();
			UUID busRouteID = busRoute.getBusRouteID();
			// find nearest bus station to start location
			Location start = msg.getData().getStartLocation();
			Location startBusStopLocation = getNearestBusStopLocationTo(busRouteID, start);
			
			// find nearest bus station to destination
			Location destination = msg.getData().getDestination();
			Location finalBusStopLocation = getNearestBusStopLocationTo(busRouteID, destination);
			
			if(startBusStopLocation.equals(finalBusStopLocation))
			{
				finalBusStopLocation = getNextBusStopAfter(busRouteID, finalBusStopLocation);
			}
			List<Location> startTravelPath = getPath(busRouteID, start, startBusStopLocation);
			List<Location> destinationTravelPath = getPath(busRouteID, destination, finalBusStopLocation); 
			Collections.reverse(destinationTravelPath);
			
			int busTravelDistance = getBusTravelDistanceBetween(busRouteID, 
					startBusStopLocation, finalBusStopLocation);
			
			logger.info("replyToRequest() With the bus route " + busRouteID + " the distance is " + busTravelDistance);
			
			if(busTravelDistance < minBusTravelDistance)
			{
				selectedStartTravelPath = startTravelPath;
				selectedDestinationTravelPath = destinationTravelPath;
				selectedBusRouteID = busRouteID;
				minBusTravelDistance = busTravelDistance;
			}
		}
		
		if(selectedBusRouteID != null)
		{
			logger.info("replyToRequest() Replying to " + msg.getFrom() + " with the bus route " + selectedBusRouteID);
			
			// reply to the user with the travel paths
			BusTravelPlan travelPlan = new BusTravelPlan(selectedStartTravelPath, selectedDestinationTravelPath, 
					minBusTravelDistance, selectedBusRouteID, msg.getData().getUserNetworkAddress());
			BusTravelPlanMessage travelPlanMsg = new BusTravelPlanMessage(travelPlan, 
					network.getAddress(), mMediatorAddress);
			network.sendMessage(travelPlanMsg);
		}
	}
	
	private Location getNearestBusStopLocationTo(UUID busRouteID, Location location)
	{
		Location nearestBusStopLocation = null;
		double minDistance = Double.MAX_VALUE;
		
		BusRoute busRoute = mBusRoutes.get(busRouteID);
		List<Location> busStops = busRoute.getBusStopsLocations();
		for (Iterator<Location> iterator = busStops.iterator(); iterator.hasNext();) 
		{
			Location busStop = iterator.next();
			double distance = busStop.distanceTo(location);
			if(distance < minDistance)
			{
				nearestBusStopLocation = busStop;
				minDistance = distance;
			}
		}
		return nearestBusStopLocation;
	}
	
	private int getBusTravelDistanceBetween(UUID busRouteID, Location startBusStop, Location finalBusStop)
	{
		Map<Location, List<Location>> pathsBetweenBusStops = mBusRoutes.get(busRouteID).getPathsBetweenBusStops();
		
		int totalDistance = 0;
		while(startBusStop.equals(finalBusStop) == false)
		{
			totalDistance += pathsBetweenBusStops.get(startBusStop).size();
			startBusStop = getNextBusStopAfter(busRouteID, startBusStop);
		}
		return totalDistance;
	}
	
	private Location getNextBusStopAfter(UUID busRouteID, Location busStop)
	{
		Location nextBusStop = null;
		
		BusRoute busRoute = mBusRoutes.get(busRouteID);
		List<Location> busStopsLocations = busRoute.getBusStopsLocations();
		for (Iterator<Location> iterator = busStopsLocations.iterator(); iterator.hasNext();) 
		{
			nextBusStop = iterator.next();
			if(nextBusStop.equals(busStop))
			{
				if(iterator.hasNext())
				{
					nextBusStop = iterator.next();
				}
				else 
				{
					nextBusStop = busStopsLocations.get(0);
				}
				break;
			}
		}
		return nextBusStop;
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
		Set<Map.Entry<UUID, BusRoute>> busRoutes = mBusRoutes.entrySet();
		if((mFreeBuses.isEmpty() == false) && (busRoutes.size() > 0))
		{
			Iterator<Map.Entry<UUID, BusRoute>> iteratorBusRoutes = busRoutes.iterator();
			for (Iterator<NetworkAddress> iteratorBuses = mFreeBuses.iterator(); 
					iteratorBuses.hasNext();) 
			{
				if(iteratorBusRoutes.hasNext() == false)
				{
					iteratorBusRoutes = busRoutes.iterator();
				}
				UUID busRouteID = iteratorBusRoutes.next().getKey(); 
				sendRouteToBus(iteratorBuses.next(), busRouteID);
				iteratorBuses.remove();
			}
		}
	}
	
	private void sendRouteToBus(NetworkAddress busAddress, UUID busRouteID)
	{
		BusRouteMessage routeMsg = new BusRouteMessage(network.getAddress(), 
				busAddress, mBusRoutes.get(busRouteID)); 
		network.sendMessage(routeMsg);
	}

	public List<Location> getPath(UUID busRouteID, Location start, Location destination)
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
				List<Location> pathList = reconstructPath(traveledPaths, destination);
				pathList.remove(0);
				return pathList;
			}
			
			// check if our destination is still the best one
			Location bestBusStop = getNearestBusStopLocationTo(busRouteID, currentLocation).getLocation();
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
