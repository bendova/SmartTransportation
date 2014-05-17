package transportOffers;

import agents.User.TransportMode;
import agents.User.TransportPreference;

public abstract class TransportOffer implements ITransportOffer
{
	private TransportMode mTransportMode;
	protected double mTravelCost = 0.0;
	protected double mTravelTime = 0.0;
	private double mTravelCostScaleFactor = 1.0;
	public TransportOffer(TransportMode transportMode)
	{
		assert(transportMode != null);
		
		mTransportMode = transportMode;
	}
	
	public abstract void confirm();
	public abstract void cancel();
	public abstract void applyTransportPreference(TransportPreference preference);
	
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
	protected void scaleCost(double scale)
	{
		mTravelCost *= scale / mTravelCostScaleFactor;
		mTravelCostScaleFactor = scale;
	}
}
