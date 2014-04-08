package agents;

import gui.AgentDataForMap.AgentType;

import java.util.*;

import conversations.protocols.taxi.*;
import conversations.taxiStationTaxi.actions.*;
import conversations.taxiStationTaxi.messages.RegisterAsTaxiMessage;
import conversations.taxiStationTaxi.messages.RejectOrderMessage;
import conversations.taxiStationTaxi.messages.RevisionCompleteMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderCompleteMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderMessage;
import conversations.taxiStationTaxi.messages.TaxiStatusUpdateMessage;
import conversations.userTaxi.actions.TakeMeToDestinationAction;
import conversations.userTaxi.messages.DestinationReachedMessage;
import conversations.userTaxi.messages.RequestDestinationMessage;
import conversations.userTaxi.messages.TakeMeToDestinationMessage;
import conversations.userTaxi.messages.messageData.TaxiData;
import conversations.userTaxi.messages.messageData.TaxiOrder;
import dataStores.SimulationDataStore;
import dataStores.AgentDataStore;
import SmartTransportation.Simulation.TransportMethodSpeed;
import map.CityMap;
import uk.ac.imperial.presage2.core.environment.*;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.*;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;
import util.movement.TransportMove;

public class Taxi extends AbstractParticipant
{
	private Location mStartLocation;
	private Location mCurrentLocation;
	private Location mCurrentDestination;
	private NetworkAddress mTaxiStationAddress;
	private ParticipantLocationService mLocationService;
	private TaxiOrder mCurrentTaxiOrder;
	private Status mCurrentStatus;
	
	private int mDistanceTraveled;
	private boolean mIsRevisionComplete;
	
	private ProtocolWithTaxiStation mWithTaxiStation;
	private ProtocolWithUser mWithUser;
	
	private List<Location> mPathToTravel;
	
	private int mTimeTakenPerUnitDistance;
	
	private CityMap mCityMap;
	private SimulationDataStore mSimulationDataStore;
	
	public enum Status
	{
		AVAILABLE,
		GOING_TO_USER,
		TRANSPORTING_USER,
		GOING_TO_REVISION,
		IN_REVISION,
		BROKEN
	}
	
	public Taxi(UUID id, String name, CityMap cityMap, Location location, NetworkAddress taxiStationNetworkAddress) 
	{
		super(id, name);
		
		assert(cityMap != null);
		assert(location != null);
		assert(taxiStationNetworkAddress != null);
		
		mCityMap = cityMap;
		mStartLocation = location;
		mCurrentLocation = location;
		mCurrentStatus = Status.AVAILABLE;
		mDistanceTraveled = 0;
		mTaxiStationAddress = taxiStationNetworkAddress;
		mIsRevisionComplete = false;
		
		mPathToTravel = new ArrayList<Location>();
		
		mTimeTakenPerUnitDistance = TransportMethodSpeed.TAXI_SPEED.getTimeTakenPerUnitDistance();
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
		mWithTaxiStation.registerAsTaxi(registerMessage);
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null) 
		{
			if(input instanceof TaxiOrderMessage)
			{
				mWithTaxiStation.handleOrderMessage((TaxiOrderMessage)input);
			}
			else if (input instanceof TakeMeToDestinationMessage)
			{
				mWithUser.handleTakeMeToDestination((TakeMeToDestinationMessage)input);
			}
			else if (input instanceof RevisionCompleteMessage)
			{
				mWithTaxiStation.handleRevisionCompleteMessage((RevisionCompleteMessage)input);
			}
		}
	}
	
	private void processOrderMessage(TaxiOrderMessage orderMessage)
	{
		logger.info("processOrderMessage() go to user at " + orderMessage.getData().getUserLocation()); 
		
		mCurrentStatus = Status.GOING_TO_USER;
		mCurrentTaxiOrder = orderMessage.getData();
		travelToUserLocation(mCurrentTaxiOrder.getUserLocation());
	}
	
	private void travelToUserLocation(Location targetLocation)
	{
		logger.info("travelToLocation() mCurrentLocation " + mCurrentLocation);
		logger.info("travelToLocation() targetLocation " + targetLocation);
		
		mCurrentDestination = targetLocation;
		if(mCurrentLocation.equals(targetLocation))
		{
			onDestinationReached();
			return;
		}
		
		mPathToTravel = mCityMap.getPath(mCurrentLocation, mCurrentDestination);
		moveTo(mPathToTravel.remove(0));
	}
	
	private void sendRejectOrderMessage(TaxiOrderMessage msg)
	{
		logger.info("sendRejectOrderMessage() msg " + msg);
		RejectOrderMessage rejectOrderMessage = new RejectOrderMessage(network.getAddress(), mTaxiStationAddress);
		mWithTaxiStation.rejectOrder(rejectOrderMessage);
	}
	
	private void moveTo(Location targetLocation)
	{
		TransportMove move = new TransportMove(targetLocation, 
				mTimeTakenPerUnitDistance);
		try 
		{
			environment.act(move, getID(), authkey);
			if(mCurrentStatus.equals(Status.TRANSPORTING_USER))
			{
				environment.act(move, mCurrentTaxiOrder.getUserID(), mCurrentTaxiOrder.getUserAuthKey());
			}
		}
		catch (ActionHandlingException e) 
		{
			logger.warn("Error while moving!", e);
		}
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		updateLocation();
		switch (mCurrentStatus) 
		{
		case GOING_TO_USER:
		case GOING_TO_REVISION:
		case TRANSPORTING_USER:
			if(mPathToTravel.size() > 0)
			{
				moveTo(mPathToTravel.remove(0));
			}
			break;
		}
	}
	
	private void updateLocation()
	{
		Location currentLocation = mLocationService.getAgentLocation(getID());
		if(mCurrentLocation.equals(currentLocation) == false)
		{
			logger.info("UpdateLocation() currentLocation " + currentLocation);
			
			mCurrentLocation = currentLocation;
			
			if((mCurrentTaxiOrder != null) && (mCurrentLocation.equals(mCurrentDestination)))
			{
				mCurrentDestination = null;
				onDestinationReached();
			}
		}
	}
	
	private void onDestinationReached()
	{
		logger.info("onDestinationReached() mLocation " + mCurrentLocation);
		
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
		}
	}
	
	private void requestDestinationFrom(NetworkAddress fromUser)
	{
		logger.info("requestDestinationFrom() fromUser " + fromUser);
		
		RequestDestinationMessage requestDestination = new RequestDestinationMessage(
				network.getAddress(), fromUser);
		mWithUser.requestDestination(requestDestination);
	}
	
	private void processOrderMessage(TakeMeToDestinationMessage takeMeToDestinationMessage)
	{
		logger.info("processOrderMessage() takeMeToDestination " + takeMeToDestinationMessage.getData());
		
		if(takeMeToDestinationMessage.getFrom().equals(mCurrentTaxiOrder.getUserNetworkAddress()))
		{
			mCurrentStatus = Status.TRANSPORTING_USER;
			transportUserToLocation(takeMeToDestinationMessage.getData());
		}
	}
	
	private void transportUserToLocation(Location targetLocation)
	{
		logger.info("transportUserToLocation() mCurrentLocation " + mCurrentLocation);
		logger.info("transportUserToLocation() targetLocation " + targetLocation);
		
		mCurrentDestination = targetLocation;
		if(mCurrentLocation.equals(targetLocation))
		{
			onDestinationReached();
			return;
		}
		
		mPathToTravel = mCityMap.getPath(mCurrentLocation, mCurrentDestination);
		moveTo(mPathToTravel.remove(0));
	}
	
	private void notifyStationOfOrderCompleted()
	{
		logger.info("notifyStationOfOrderCompleted()");
				
		TaxiOrderCompleteMessage taxiOrderCompleteMessage = new TaxiOrderCompleteMessage
				(getID(), network.getAddress(), mTaxiStationAddress);
		mWithTaxiStation.reportOrderComplete(taxiOrderCompleteMessage);
	}
	
	private void notifyUserOfDestinationReached()
	{
		logger.info("notifyUserOfDestinationReached()");
		
		DestinationReachedMessage msg = new DestinationReachedMessage(
				"We have reached your destination, sir!", 
				network.getAddress(), mCurrentTaxiOrder.getUserNetworkAddress());
		mWithUser.reportDestinationReached(msg);
	}
	
	private void notifyStationOfStatusUpdate()
	{
		logger.info("notifyStationOfStatusUpdate()");
		
		TaxiStatusUpdateMessage msg = new TaxiStatusUpdateMessage(mCurrentStatus,
				network.getAddress(), mTaxiStationAddress);
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
		AgentDataStore dataStore = new AgentDataStore(getName(), getID(), 
				AgentType.TAXI_CAB, mStartLocation);
		mSimulationDataStore.addAgentDataStore(getID(), dataStore);
	}
}
