package transportOffers;

import agents.User.TransportMode;

public abstract class TransportOffer
{
	private TransportMode mTransportMode;
	protected double mTravelCost = 0.0;
	protected double mTravelTime = 0.0;
	public TransportOffer(TransportMode transportMode)
	{
		assert(transportMode != null);
		
		mTransportMode = transportMode;
	}
	
	public abstract void confirm();
	public abstract void cancel();
	
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
