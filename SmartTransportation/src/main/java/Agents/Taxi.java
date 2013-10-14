package agents;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import messageData.TaxiData;
import messageData.TaxiOrder;
import messages.RegisterAsTaxiMessage;
import messages.RequestDestinationMessage;
import messages.TakeMeToDestinationMessage;
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
	private NetworkAddress mTaxiStationAddress;
	private ParticipantLocationService mLocationService;
	private TaxiOrder mCurrentTaxiOrder;
	
	public Taxi(UUID id, String name, Location location, NetworkAddress taxiStationNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mTaxiStationAddress = taxiStationNetworkAddress;
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
				// TODO check if free before processing
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
		mCurrentTaxiOrder = orderMessage.getData();
		moveToLocation(mCurrentTaxiOrder.getUserLocation());
	}
	
	private void moveToLocation(Location targetLocation)
	{
		logger.info("moveToLocation() targetLocation " + targetLocation); 
		Move move = new Move(mLocation.getMoveTo(targetLocation));
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
			if((mCurrentTaxiOrder != null) && (mLocation.equals(mCurrentTaxiOrder.getUserLocation())))
			{
				onUserLocationReached();
			}
		}
	}
	
	private void onUserLocationReached()
	{
		logger.info("OnUserLocationReached()");
		
 		requestDestinationFrom(mCurrentTaxiOrder.getUserNetworkAddress());
	}
	
	private void requestDestinationFrom(NetworkAddress fromUser)
	{
		RequestDestinationMessage requestDestination = new RequestDestinationMessage(
				network.getAddress(), fromUser);
		network.sendMessage(requestDestination);
	}
	
	private void processOrderMessage(TakeMeToDestinationMessage takeMeToDestinationMessage)
	{
		logger.info("processOrderMessage() takeMeToDestination " + takeMeToDestinationMessage.getData());
		
		if(takeMeToDestinationMessage.getFrom().equals(mCurrentTaxiOrder.getUserNetworkAddress()))
		{
			moveToLocation(takeMeToDestinationMessage.getData());
		}
	}
}
