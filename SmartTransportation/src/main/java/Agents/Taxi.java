package agents;

import java.util.*;

import conversations.protocols.taxi.*;
import conversations.taxistationtaxi.actions.*;
import conversations.usertaxi.actions.TakeMeToDestinationAction;
import SmartTransportation.Simulation;
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
		moveToLocation(mCurrentTaxiOrder.getUserLocation());
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
	
	private void moveToLocation(Location targetLocation)
	{
		logger.info("moveToLocation() mLocation " + mLocation);
		logger.info("moveToLocation() targetLocation " + targetLocation);
		
		mCurrentDestination = targetLocation;
		if(mLocation.equals(targetLocation))
		{
			onDestinationReached();
			return;
		}
		
		Move move = new Move(mLocation.getMoveTo(mCurrentDestination));
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
		
		Move move = new Move(mLocation.getMoveTo(mCurrentDestination));
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
