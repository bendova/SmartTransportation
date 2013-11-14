package messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class RejectOrderMessage extends UnicastMessage<String>
{
	public RejectOrderMessage(NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REFUSE, from, to, new TimeStamp());
		data = "Can't handle the order at the moment, sir!";
	}
}
