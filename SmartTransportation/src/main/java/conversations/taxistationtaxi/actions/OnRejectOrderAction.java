package conversations.taxistationtaxi.actions;

import messages.RejectOrderMessage;
import util.protocols.Action;
import util.protocols.Transition;

public abstract class OnRejectOrderAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((RejectOrderMessage)event);
	}
	
	public abstract void processMessage(RejectOrderMessage msg);
}