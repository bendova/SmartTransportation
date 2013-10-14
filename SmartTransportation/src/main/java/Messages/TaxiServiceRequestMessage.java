package messages;

import messageData.TaxiServiceRequest;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiServiceRequestMessage extends UnicastMessage<TaxiServiceRequest>
										implements Comparable<TaxiServiceRequestMessage>
{	
	public TaxiServiceRequestMessage(TaxiServiceRequest request, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.PROPOSE, from, to, new TimeStamp());
		
		assert(request != null);
		data = request;		
	}

	@Override
	public int compareTo(TaxiServiceRequestMessage otherMessage) 
	{
		if(otherMessage != null)
		{
			return (timestamp.intValue() - otherMessage.timestamp.intValue());
		}
		return 1;
	}
}
