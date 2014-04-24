package transportOffers;

import SmartTransportation.Simulation.TransportMethodCost;
import SmartTransportation.Simulation.TransportMethodSpeed;
import agents.User.TransportMode;
import agents.User.TransportPreference;
import conversations.userBusStation.BusTravelPlanMessage;

public class BusTransportOffer extends TransportOffer
{
	private BusTravelPlanMessage mBusTravelPlanMessage;
	
	public BusTransportOffer(BusTravelPlanMessage busMessage)
	{
		super(TransportMode.TAKE_BUS);
		
		assert(busMessage != null);
		mBusTravelPlanMessage = busMessage;
		
		int travelToBusStopDistance = busMessage.getData().getPathToFirstBusStop().size();
		int travelToDestinationDistance = busMessage.getData().getPathToDestination().size();
		double busTravelDistance = (double)busMessage.getData().getBusTravelDistance();
		// this is the worst case scenario - there is only one bus on the route,
		// and it has just left the station
		double onFootTravelTime = (double)(travelToBusStopDistance + travelToDestinationDistance) 
				/ TransportMethodSpeed.WALKING_SPEED.getSpeed();
		
		// this is the worst case scenario - there is only one bus on the route,
		// and it has just left the station
		double onBusTravelTime = 2 * busTravelDistance * TransportMethodSpeed.BUS_SPEED.getTimeTakenPerUnitDistance();
		
		mTravelTime = onFootTravelTime + onBusTravelTime;
		mTravelCost = busTravelDistance * TransportMethodCost.BUS_COST.getCost();
	}

	public BusTravelPlanMessage getBusTravelPlanMessage()
	{
		return mBusTravelPlanMessage;
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
		scaleCost(preference.getBusCostScaling());
	}
}
