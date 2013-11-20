package conversations.taxistationtaxi.actions;

import messages.TaxiStatusUpdateMessage;
import util.protocols.Action;
import util.protocols.Transition;

public abstract class OnTaxiStatusUpdateAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((TaxiStatusUpdateMessage)event);
	}
	
	public abstract void processMessage(TaxiStatusUpdateMessage msg);
}
