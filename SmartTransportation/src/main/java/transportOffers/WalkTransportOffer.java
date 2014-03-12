package transportOffers;

import java.util.List;

import SmartTransportation.Simulation.TransportMethodCost;
import SmartTransportation.Simulation.TransportMethodSpeed;
import agents.User.TransportMode;
import uk.ac.imperial.presage2.util.location.Location;

public class WalkTransportOffer extends TransportOffer<List<Location>>
{
	public WalkTransportOffer(List<Location> walkPath)
	{
		super(walkPath, TransportMode.WALKING);
		
		double onFootTravelDistance = walkPath.size();
		mTravelCost = onFootTravelDistance * TransportMethodCost.WALKING_COST.getCost();
		mTravelTime = onFootTravelDistance * TransportMethodSpeed.WALKING_SPEED.getTimeTakenPerUnitDistance();
	}
	
	@Override
	public void confirm() 
	{
		// do nothing, for now
	}

	@Override
	public void cancel() 
	{
		// do nothing, for now
	}
}
