package transportOffers;

import agents.User.TransportMode;
import agents.User.TransportPreference;

public abstract class TransportOffer<T>
{
	private TransportMode mTransportMode;
	private T mTransportPlan;
	protected double mTravelCost = 0.0;
	protected double mTravelTime = 0.0;
	public TransportOffer(T transportPlan, TransportMode transportMode)
	{
		assert(transportPlan != null);
		assert(transportMode != null);
		
		mTransportMode = transportMode;
		mTransportPlan = transportPlan;
	}
	
	public abstract void confirm();
	public abstract void cancel();
	
	public T getTransportPlan()
	{
		return mTransportPlan;
	}
	public TransportMode getTransportMode()
	{
		return mTransportMode;
	}
	
	public double getCost() 
	{
		return mTravelCost;
	}
	public double getTravelTime() 
	{
		return mTravelTime;
	}
	public void setCostScaleFactor(double scale)
	{
		mTravelCost *= scale;
	}
}
