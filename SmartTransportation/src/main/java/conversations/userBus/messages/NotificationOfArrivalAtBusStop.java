package conversations.userBus.messages;

import conversations.userBus.messages.messageData.IBusStopArrivalNotification;
import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class NotificationOfArrivalAtBusStop extends UnicastMessage<IBusStopArrivalNotification>
{
	public NotificationOfArrivalAtBusStop(IBusStopArrivalNotification notification, 
			NetworkAddress from, NetworkAddress to)
	{
		super(Performative.INFORM, from, to, new TimeStamp());
		
		assert(notification != null);
		data = notification;
	}
}
