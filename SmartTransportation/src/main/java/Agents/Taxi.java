package agents;

import java.util.Set;
import java.util.UUID;

import messageData.TaxiData;
import messageData.TaxiOrder;
import messages.DestinationReachedMessage;
import messages.RegisterAsTaxiMessage;
import messages.RequestDestinationMessage;
import messages.TakeMeToDestinationMessage;
import messages.TaxiOrderCompleteMessage;
import messages.TaxiOrderMessage;

import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.Move;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class Taxi extends AbstractParticipant
{
	private Location mLocation;
	private Location mCurrentDestination;
	private NetworkAddress mTaxiStationAddress;
	private ParticipantLocationService mLocationService;
	private TaxiOrder mCurrentTaxiOrder;
	private Task mCurrentTask;
	
	private enum Task
	{
		IDLE,
		GO_TO_USER,
		TRANSPORT_USER
	}
	
	public Taxi(UUID id, String name, Location location, NetworkAddress taxiStationNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mTaxiStationAddress = taxiStationNetworkAddress;
		mCurrentTask = Task.IDLE;
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return ss;
	}
	
	@Override
	public void initialise() 
	{
		super.initialise();
		
		registerToTaxiServiceProvider();
		initializeLocationService();
	}
	
	private void registerToTaxiServiceProvider()
	{
		logger.info("RegisterToTaxiServiceProvider()");
		
		TaxiData taxiData = new TaxiData(getID(), network.getAddress());
		RegisterAsTaxiMessage registerMessage = new 
				RegisterAsTaxiMessage(taxiData, network.getAddress(), mTaxiStationAddress);
		network.sendMessage(registerMessage);
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
		if(input != null) 
		{
			if(input instanceof TaxiOrderMessage)
			{
				assert(mCurrentTask == Task.IDLE);
				processOrderMessage((TaxiOrderMessage)input);
			}
			else if (input instanceof TakeMeToDestinationMessage)
			{
				processOrderMessage((TakeMeToDestinationMessage)input);
			}
		}
	}
	
	private void processOrderMessage(TaxiOrderMessage orderMessage)
	{
		logger.info("processOrder()"); 
		
		mCurrentTask = Task.GO_TO_USER;
		mCurrentTaxiOrder = orderMessage.getData();
		moveToLocation(mCurrentTaxiOrder.getUserLocation());
	}
	
	private void moveToLocation(Location targetLocation)
	{
		logger.info("moveToLocation() targetLocation " + targetLocation);
		
		mCurrentDestination = targetLocation;
		Move move = new Move(mLocation.getMoveTo(mCurrentDestination));
		try 
		{
			environment.act(move, getID(), authkey);		
		} 
		catch (ActionHandlingException e) 
		{
			logger.warn("Error while moving!", e);
			// TODO reset taxi state if this exception occurs,
			// so that we may at least be able to process other 
			// orders
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
	}
	
	private void onDestinationReached()
	{
		logger.info("onDestinationReached()");
		
		switch (mCurrentTask) 
		{
		case GO_TO_USER:
	 		requestDestinationFrom(mCurrentTaxiOrder.getUserNetworkAddress());
			break;
		case TRANSPORT_USER:
			notifyUserOfDestinationReached();
			notifyStationOfOrderCompleted();
			mCurrentTaxiOrder = null;
			mCurrentTask = Task.IDLE;
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
		network.sendMessage(requestDestination);
	}
	
	private void processOrderMessage(TakeMeToDestinationMessage takeMeToDestinationMessage)
	{
		logger.info("processOrderMessage() takeMeToDestination " + takeMeToDestinationMessage.getData());
		
		if(takeMeToDestinationMessage.getFrom().equals(mCurrentTaxiOrder.getUserNetworkAddress()))
		{
			mCurrentTask = Task.TRANSPORT_USER;
			moveToLocation(takeMeToDestinationMessage.getData());
		}
	}
	
	private void notifyStationOfOrderCompleted()
	{
		logger.info("notifyStationOfOrderCompleted()");
				
		TaxiOrderCompleteMessage taxiOrderCompleteMessage = new TaxiOrderCompleteMessage
				(getID(), network.getAddress(), mTaxiStationAddress);
		network.sendMessage(taxiOrderCompleteMessage);
	}
	
	private void notifyUserOfDestinationReached()
	{
		logger.info("notifyUserOfDestinationReached()");
		
		DestinationReachedMessage msg = new DestinationReachedMessage(
				"We have reached your destination, sir!", 
				network.getAddress(), mCurrentTaxiOrder.getUserNetworkAddress());
		network.sendMessage(msg);
	}
}
