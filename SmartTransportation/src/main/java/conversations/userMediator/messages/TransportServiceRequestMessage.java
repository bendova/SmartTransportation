package conversations.userMediator.messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TransportServiceRequestMessage extends UnicastMessage<ITransportServiceRequest>
										implements Comparable<TransportServiceRequestMessage>
{	
	public TransportServiceRequestMessage(ITransportServiceRequest request, NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		assert(request != null);
		data = request;		
	}

	@Override
	public int compareTo(TransportServiceRequestMessage otherMessage) 
	{
		if(otherMessage != null)
		{
			return (otherMessage.timestamp.intValue() - timestamp.intValue());
		}
		return 1;
	}
}
