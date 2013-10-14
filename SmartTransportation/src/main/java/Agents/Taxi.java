package Agents;

import java.util.Set;
import java.util.UUID;

import Messages.RegisterAsTaxiMessage;
import Messages.TaxiData;
import Messages.TaxiOrder;
import Messages.TaxiOrderMessage;

import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
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
	
	public Taxi(UUID id, String name, Location location, NetworkAddress taxiStationNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mTaxiStationAddress = taxiStationNetworkAddress;
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() {
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return ss;
	}
	
	@Override
	public void initialise() 
	{
		super.initialise();
		
		registerToTaxiServiceProvider();
	}
	
	private void registerToTaxiServiceProvider()
	{
		logger.info("RegisterToTaxiServiceProvider()");
		
		TaxiData taxiData = new TaxiData(getID(), network.getAddress());
		RegisterAsTaxiMessage registerMessage = new 
				RegisterAsTaxiMessage(taxiData, network.getAddress(), mTaxiStationAddress);
		network.sendMessage(registerMessage);
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null) 
		{
			if(input instanceof TaxiOrderMessage)
			{
				// TODO check if free before processing
				processOrder((TaxiOrderMessage)input);
			}
		}
	}
	
	private void processOrder(TaxiOrderMessage orderMessage)
	{
		logger.info("processOrder()"); 
		TaxiOrder taxiOrder = orderMessage.getData();
		moveToLocation(taxiOrder.getUserLocation());
		// TODO interact with client
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
		logger.info("moveToLocation() mLocation " + mLocation); 
	}
}
