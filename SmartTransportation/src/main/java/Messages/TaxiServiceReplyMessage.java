package messages;

import messageData.TaxiServiceReply;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiServiceReplyMessage extends UnicastMessage<TaxiServiceReply>
{
	public TaxiServiceReplyMessage(TaxiServiceReply taxiServiceReply, 
									NetworkAddress from, NetworkAddress to) 
	{
		super(Performative.ACCEPT_PROPOSAL, from, to, new TimeStamp());
		
		assert(taxiServiceReply != null);
		data = taxiServiceReply;
	}
}
