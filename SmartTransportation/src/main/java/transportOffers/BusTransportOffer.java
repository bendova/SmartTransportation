package transportOffers;

import SmartTransportation.Simulation.TransportMethodCost;
import SmartTransportation.Simulation.TransportMethodSpeed;
import agents.User.TransportMode;
import conversations.userBusStation.BusTravelPlanMessage;

public class BusTransportOffer extends TransportOffer<BusTravelPlanMessage>
{
	public BusTransportOffer(BusTravelPlanMessage busOffer)
	{
		super(busOffer, TransportMode.TAKE_BUS);
		
		int travelToBusStopDistance = busOffer.getData().getPathToFirstBusStop().size();
		int travelToDestinationDistance = busOffer.getData().getPathToDestination().size();
		double busTravelDistance = (double)busOffer.getData().getBusTravelDistance();
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
