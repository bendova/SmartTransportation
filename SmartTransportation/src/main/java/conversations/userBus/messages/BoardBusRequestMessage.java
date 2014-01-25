package conversations.userBus.messages;

import conversations.userBus.messages.messageData.IBoardBusRequest;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class BoardBusRequestMessage extends UnicastMessage<IBoardBusRequest>
{
	public BoardBusRequestMessage(IBoardBusRequest request, 
			NetworkAddress from, NetworkAddress to)
	{
		super(Performative.INFORM, from, to, new TimeStamp());
		
		assert(request != null);
		data = request;
	}
}
