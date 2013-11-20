package conversations.taxistationtaxi.actions;

import messages.TaxiOrderMessage;
import util.protocols.Action;
import util.protocols.Transition;

public abstract class OnReceiveOrderAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((TaxiOrderMessage)event);
	}
	
	public abstract void processMessage(TaxiOrderMessage msg);
}
