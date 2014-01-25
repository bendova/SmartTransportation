package conversations.userBus.messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class UnBoardBusRequestMessage extends UnicastMessage<String>
{
	public UnBoardBusRequestMessage(String msg, 
			NetworkAddress from, NetworkAddress to)
	{
		super(Performative.INFORM, from, to, new TimeStamp());
		
		assert(msg != null);
		data = msg;
	}
}
