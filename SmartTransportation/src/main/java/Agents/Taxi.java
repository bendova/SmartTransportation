package agents;

import java.util.*;

import com.google.inject.Inject;

import conversations.protocols.taxi.*;
import conversations.taxistationtaxi.actions.*;
import conversations.usertaxi.actions.TakeMeToDestinationAction;
import SmartTransportation.Simulation;
import map.CityMap;
import messages.*;
import messages.messageData.*;
import uk.ac.imperial.presage2.core.environment.*;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.*;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class Taxi extends AbstractParticipant
{
	private Location mLocation;
	private Location mCurrentDestination;
	private NetworkAddress mTaxiStationAddress;
	private ParticipantLocationService mLocationService;
	private TaxiOrder mCurrentTaxiOrder;
	private Status mCurrentStatus;
	
	private int mDistanceTraveled;
	private boolean mIsRevisionComplete;
	
	private ArrayList<Location> mLocations; 
	
	private ProtocolWithTaxiStation mWithTaxiStation;
	private ProtocolWithUser mWithUser;
	
	private List<Location> mPathToTravel;
	
	@Inject
	private CityMap mCityMap;
	
	public enum Status
	{
		AVAILABLE,
		GOING_TO_USER,
		TRANSPORTING_USER,
		GOING_TO_REVISION,
		IN_REVISION,
		BROKEN
	}
	
	public Taxi(UUID id, String name, Location location, NetworkAddress taxiStationNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mCurrentStatus = Status.AVAILABLE;
		mDistanceTraveled = 0;
		mTaxiStationAddress = taxiStationNetworkAddress;
		mIsRevisionComplete = false;
		
		mLocations = new ArrayList<Location>();
		mPathToTravel = new LinkedList<Location>();
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return ss;
	}
	
	public Status getCurrentStatus()
	{
		return mCurrentStatus;
	}
	
	public int getDistanceTraveled()
	{
		return mDistanceTraveled;
	}
	
	public boolean isRevisionComplete()
	{
		return mIsRevisionComplete;
	}
	
	public void resetDistanceTraveled()
	{
		mDistanceTraveled = 0;
	}
	
	public void goToRevision()
	{
		logger.info("goToRevision() mDistanceTraveled " + mDistanceTraveled);
		
		assert(mCurrentStatus == Status.AVAILABLE);
		
		// TODO Add another type of agent
		// that dictates when our revision is
		// complete
		mIsRevisionComplete = true;
		updateStatusAndNotify(Status.IN_REVISION);
	}
	
	public void goToWork()
	{
		logger.info("goToWork()");
		
		assert(mCurrentStatus == Status.IN_REVISION);
		
		updateStatusAndNotify(Status.AVAILABLE);
	}
	
	private void updateStatusAndNotify(Status newStatus)
	{
		if(newStatus != mCurrentStatus)
		{
			mCurrentStatus = newStatus;
			notifyStationOfStatusUpdate();
		}
	}
	
	@Override
	public void initialise() 
	{
		super.initialise();
		
		initializeLocationService();
		initialiseProtocols();
		registerToTaxiServiceProvider();
		
		logger.info("mCityMap " + mCityMap);
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
	
	private void initialiseProtocols()
	{
		initProtocolWithUser();
		initProtocolWithTaxiStation();
	}
	
	private void initProtocolWithUser()
	{
		TakeMeToDestinationAction action = new TakeMeToDestinationAction() 
		{
			@Override
			public void processMessage(TakeMeToDestinationMessage msg) 
			{
				processOrderMessage(msg);
			}
		};
		
		mWithUser = new ProtocolWithUser(network);
		mWithUser.init(action);
	}
	
	private void initProtocolWithTaxiStation()
	{
		OnReceiveOrderAction onReceiveOrderAction = new OnReceiveOrderAction()
		{
			@Override
			public void processMessage(TaxiOrderMessage msg)
			{
				if(mCurrentStatus != Status.AVAILABLE)
				{
					logger.info("processInput() mCurrentStatus " + mCurrentStatus);
					sendRejectOrderMessage(msg);
				}
				else
				{				
					processOrderMessage(msg);
				}
			}
		};
		
		mWithTaxiStation = new ProtocolWithTaxiStation(network);
		mWithTaxiStation.init(onReceiveOrderAction);
	}
	
	private void registerToTaxiServiceProvider()
	{
		logger.info("RegisterToTaxiServiceProvider()");
		
		TaxiData taxiData = new TaxiData(getID(), network.getAddress());
		RegisterAsTaxiMessage registerMessage = new 
				RegisterAsTaxiMessage(taxiData, network.getAddress(), mTaxiStationAddress);
		//network.sendMessage(registerMessage);
		mWithTaxiStation.registerAsTaxi(registerMessage);
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null) 
		{
			if(input instanceof TaxiOrderMessage)
			{
				/*
				if(mCurrentStatus != Status.AVAILABLE)
				{
					logger.info("processInput() mCurrentStatus " + mCurrentStatus);
					sendRejectOrderMessage((TaxiOrderMessage)input);
				}
				else
				{				
					processOrderMessage((TaxiOrderMessage)input);
				}
				*/
				mWithTaxiStation.handleOrderMessage((TaxiOrderMessage)input);
			}
			else if (input instanceof TakeMeToDestinationMessage)
			{
				//processOrderMessage((TakeMeToDestinationMessage)input);
				
				mWithUser.handleTakeMeToDestination((TakeMeToDestinationMessage)input);
			}
			else if (input instanceof RevisionCompleteMessage)
			{
				//handleRevisionComplete((RevisionCompleteMessage)input);
				mWithTaxiStation.handleRevisionCompleteMessage((RevisionCompleteMessage)input);
			}
		}
	}
	
	private void processOrderMessage(TaxiOrderMessage orderMessage)
	{
		logger.info("processOrderMessage() go to user at " + orderMessage.getData().getUserLocation()); 
		
		mCurrentStatus = Status.GOING_TO_USER;
		mCurrentTaxiOrder = orderMessage.getData();
		travelToLocation(mCurrentTaxiOrder.getUserLocation());
	}
	
	private void sendRejectOrderMessage(TaxiOrderMessage msg)
	{
		logger.info("sendRejectOrderMessage() msg " + msg);
		RejectOrderMessage rejectOrderMessage = new RejectOrderMessage(network.getAddress(), mTaxiStationAddress);
		//network.sendMessage(rejectOrderMessage);
		mWithTaxiStation.rejectOrder(rejectOrderMessage);
	}
	
	private void handleRevisionComplete(RevisionCompleteMessage msg)
	{
		logger.info("handleRevisionComplete() " + msg.getData()); 
		
		mIsRevisionComplete = true;
	}
	
	private void travelToLocation(Location targetLocation)
	{
		logger.info("moveToLocation() mLocation " + mLocation);
		logger.info("moveToLocation() targetLocation " + targetLocation);
		
		mCurrentDestination = targetLocation;
		if(mLocation.equals(targetLocation))
		{
			onDestinationReached();
			return;
		}
		
		mPathToTravel = getPathTo(mCurrentDestination);
		moveTo(mPathToTravel.remove(0));
	}
	
	private void moveTo(Location targetLocation)
	{
		Move move = new Move(mLocation.getMoveTo(targetLocation));
		try 
		{
			environment.act(move, getID(), authkey);
			mDistanceTraveled += move.getNorm();
		}
		catch (ActionHandlingException e) 
		{
			logger.warn("Error while moving!", e);
		}
	}
	
	private void transportToLocation(Location targetLocation)
	{
		logger.info("transportToLocation() mLocation " + mLocation);
		logger.info("transportToLocation() targetLocation " + targetLocation);
		
		mCurrentDestination = targetLocation;
		if(mLocation.equals(targetLocation))
		{
			onDestinationReached();
			return;
		}
		
		mPathToTravel = getPathTo(mCurrentDestination);
		transportTo(mPathToTravel.remove(0));
	}
	
	private void transportTo(Location targetLocation)
	{
		Move move = new Move(mLocation.getMoveTo(targetLocation));
		try 
		{
			environment.act(move, getID(), authkey);
			environment.act(move, mCurrentTaxiOrder.getUserID(), mCurrentTaxiOrder.getUserAuthKey());
			mDistanceTraveled += move.getNorm();
		}
		catch (ActionHandlingException e) 
		{
			logger.warn("Error while moving!", e);
		}
	}
	
	public List<Location> getPathTo(Location destination)
	{
		HashSet<Location> evaluatedLocations = new HashSet<Location>();
		HashMap<Location, Location> traveledPaths = new HashMap<Location, Location>();
		HashSet<Location> locationsToEvaluate = new HashSet<Location>();
		locationsToEvaluate.add(mLocation);
		
		HashMap<Location, Integer> globalCostMap = new HashMap<Location, Integer>();
		globalCostMap.put(mLocation, 0);
		HashMap<Location, Integer> estimatedCostMap = new HashMap<Location, Integer>();
		estimatedCostMap.put(mLocation, Integer.valueOf((int)mLocation.distanceTo(destination)));
		
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
				return reconstructPath(traveledPaths, destination);
			}
			
			locationsToEvaluate.remove(currentLocation);
			evaluatedLocations.add(currentLocation);
			// evaluate the neighbors
			for (Iterator<Location> iterator2 = getNeighboringLocations(currentLocation).iterator(); 
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
		return null;
	}
	
	private List<Location> getNeighboringLocations(Location location)
	{
		List<Location> neighbors = new LinkedList<Location>();
		
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
		
		/*
		if((mCityMap.isValidLocation(--x, ++y)))
		{
			neighbors.add(new Location(x, y));
		}
		++x;
		--y;
		if(mCityMap.isValidLocation(++x, ++y))
		{
			neighbors.add(new Location(x, y));
		}
		--x;
		--y;
		if((mCityMap.isValidLocation(--x, --y)))
		{
			neighbors.add(new Location(x, y));
		}
		++x;
		++y;
		if(mCityMap.isValidLocation(++x, --y))
		{
			neighbors.add(new Location(x, y));
		}
		--x;
		++y;
		*/
		return neighbors;
	}
	
	private List<Location> reconstructPath(HashMap<Location, Location> traveledPaths, Location currentLocation)
	{
		if(traveledPaths.containsKey(currentLocation))
		{
			List<Location> path = reconstructPath(traveledPaths, traveledPaths.get(currentLocation));
			path.add(currentLocation);
			return path;
		}
		else 
		{
			LinkedList<Location> path = new LinkedList<Location>();
			path.add(currentLocation);
			return path;
		}
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		updateLocation();
	}
	
	private void updateLocation()
	{
		Location currentLocation = mLocationService.getAgentLocation(getID());
		if(mLocation.equals(currentLocation) == false)
		{
			logger.info("UpdateLocation() currentLocation " + currentLocation);
			
			mLocation = currentLocation;
			if((mCurrentTaxiOrder != null) && (mLocation.equals(mCurrentDestination)))
			{
				mCurrentDestination = null;
				onDestinationReached();
			}
		}
		mLocations.add(currentLocation);
		
		switch (mCurrentStatus) 
		{
		case TRANSPORTING_USER:
			if(mPathToTravel.size() > 0)
			{
				transportTo(mPathToTravel.remove(0));
			}
			break;
		case GOING_TO_USER:
		case GOING_TO_REVISION:
			if(mPathToTravel.size() > 0)
			{
				moveTo(mPathToTravel.remove(0));
			}
			break;
		}
	}
	
	private void onDestinationReached()
	{
		logger.info("onDestinationReached() mLocation " + mLocation);
		
		switch (mCurrentStatus) 
		{
		case GOING_TO_USER:
	 		requestDestinationFrom(mCurrentTaxiOrder.getUserNetworkAddress());
			break;
		case TRANSPORTING_USER:
			notifyUserOfDestinationReached();
			notifyStationOfOrderCompleted();
			mCurrentTaxiOrder = null;
			mCurrentStatus = Status.AVAILABLE;
			break;
		default:
			break;
		}
	}
	
	private void requestDestinationFrom(NetworkAddress fromUser)
	{
		logger.info("requestDestinationFrom() fromUser " + fromUser);
		
		RequestDestinationMessage requestDestination = new RequestDestinationMessage(
				network.getAddress(), fromUser);
		//network.sendMessage(requestDestination);
		mWithUser.requestDestination(requestDestination);
	}
	
	private void processOrderMessage(TakeMeToDestinationMessage takeMeToDestinationMessage)
	{
		logger.info("processOrderMessage() takeMeToDestination " + takeMeToDestinationMessage.getData());
		
		if(takeMeToDestinationMessage.getFrom().equals(mCurrentTaxiOrder.getUserNetworkAddress()))
		{
			mCurrentStatus = Status.TRANSPORTING_USER;
			transportToLocation(takeMeToDestinationMessage.getData());
		}
	}
	
	private void notifyStationOfOrderCompleted()
	{
		logger.info("notifyStationOfOrderCompleted()");
				
		TaxiOrderCompleteMessage taxiOrderCompleteMessage = new TaxiOrderCompleteMessage
				(getID(), network.getAddress(), mTaxiStationAddress);
		//network.sendMessage(taxiOrderCompleteMessage);
		mWithTaxiStation.reportOrderComplete(taxiOrderCompleteMessage);
	}
	
	private void notifyUserOfDestinationReached()
	{
		logger.info("notifyUserOfDestinationReached()");
		
		DestinationReachedMessage msg = new DestinationReachedMessage(
				"We have reached your destination, sir!", 
				network.getAddress(), mCurrentTaxiOrder.getUserNetworkAddress());
		//network.sendMessage(msg);
		mWithUser.reportDestinationReached(msg);
	}
	
	private void notifyStationOfStatusUpdate()
	{
		logger.info("notifyStationOfStatusUpdate()");
		
		TaxiStatusUpdateMessage msg = new TaxiStatusUpdateMessage(mCurrentStatus,
				network.getAddress(), mTaxiStationAddress);
		//network.sendMessage(msg);
		mWithTaxiStation.sendStatusUpdate(msg);
	}
	
	@Override
	public boolean equals(final	Object other)
	{
		if(this == other)
		{
			return true;
		}
		
		if (other instanceof Taxi)
		{
			return ((Taxi)other).getID().equals(getID());
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return getID().hashCode();
	}
	
	@Override
	public void onSimulationComplete()
	{
		Simulation.addTaxiLocations(getName(), mLocations);
	}
}
