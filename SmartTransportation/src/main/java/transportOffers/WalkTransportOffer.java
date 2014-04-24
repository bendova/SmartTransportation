package transportOffers;

import java.util.List;

import SmartTransportation.Simulation.TransportMethodCost;
import SmartTransportation.Simulation.TransportMethodSpeed;
import agents.User.TransportMode;
import agents.User.TransportPreference;
import uk.ac.imperial.presage2.util.location.Location;

public class WalkTransportOffer extends TransportOffer
{
	private List<Location> mWalkPath;
	public WalkTransportOffer(List<Location> walkPath)
	{
		super(TransportMode.WALKING);
		
		assert(walkPath != null);
		mWalkPath = walkPath;
		
		double onFootTravelDistance = walkPath.size();
		mTravelCost = onFootTravelDistance * TransportMethodCost.WALKING_COST.getCost();
		mTravelTime = onFootTravelDistance * TransportMethodSpeed.WALKING_SPEED.getTimeTakenPerUnitDistance();
	}
	
	public List<Location> getWalkPath()
	{
		return mWalkPath;
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

	@Override
	public void applyTransportPreference(TransportPreference preference) 
	{
		scaleCost(preference.getWalkingCostScaling());
	}
}
