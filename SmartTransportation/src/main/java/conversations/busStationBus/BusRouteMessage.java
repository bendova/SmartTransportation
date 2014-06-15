package conversations.busStationBus;

import conversations.busStationBus.messageData.IBusRoute;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class BusRouteMessage extends UnicastMessage<IBusRoute>
{
	public BusRouteMessage(NetworkAddress from, NetworkAddress to,
			IBusRoute busRoute)
	{
		super(Performative.PROPOSE, from, to, new TimeStamp());
		
		assert(busRoute != null);
		data = busRoute;
	}
}
