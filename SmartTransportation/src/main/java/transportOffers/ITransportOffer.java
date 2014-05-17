package transportOffers;

import agents.User.TransportMode;

public interface ITransportOffer 
{
	public TransportMode getTransportMode();	
	public double getCost();
	public double getTravelTime();
}
