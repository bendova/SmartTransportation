package conversations.userTaxi.messages;

import conversations.taxiStationMediator.messageData.ITaxiServiceRequest;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class TaxiRequestConfirmationMessage extends UnicastMessage<ITaxiServiceRequest>
{
	public TaxiRequestConfirmationMessage(ITaxiServiceRequest taxiRequest, 
			NetworkAddress from, NetworkAddress to) 
	{
		super(Performative.CONFIRM, from, to, new TimeStamp());
	
		assert(taxiRequest != null);
		data = taxiRequest;
	}
}
