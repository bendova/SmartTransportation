package messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import uk.ac.imperial.presage2.util.location.Location;
import util.TimeStamp;

public class RequestTaxiServiceConfirmationMessage extends UnicastMessage<Location>
{
	public RequestTaxiServiceConfirmationMessage(Location taxiLocation, 
			NetworkAddress from, NetworkAddress to) 
	{
		super(Performative.QUERY_IF, from, to, new TimeStamp());
	
		assert(taxiLocation != null);
		data = taxiLocation;
	}
}
