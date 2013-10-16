package messages;

import messageData.taxiServiceRequest.TaxiServiceRequestInterface;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiServiceRequestMessage extends UnicastMessage<TaxiServiceRequestInterface>
										implements Comparable<TaxiServiceRequestMessage>
{	
	public TaxiServiceRequestMessage(TaxiServiceRequestInterface request, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		assert(request != null);
		data = request;		
	}

	@Override
	public int compareTo(TaxiServiceRequestMessage otherMessage) 
	{
		if(otherMessage != null)
		{
			return (otherMessage.timestamp.intValue() - timestamp.intValue());
		}
		return 1;
	}
}
