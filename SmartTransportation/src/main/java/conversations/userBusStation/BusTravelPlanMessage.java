package conversations.userBusStation;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.TimeStamp;

public class BusTravelPlanMessage extends UnicastMessage<IBusTravelPlan>
{
	public BusTravelPlanMessage(IBusTravelPlan plan, NetworkAddress from, 
			NetworkAddress to)
	{
		super(Performative.INFORM, from, to, new TimeStamp());
		
		assert(plan != null);
		data = plan;
	}
}
