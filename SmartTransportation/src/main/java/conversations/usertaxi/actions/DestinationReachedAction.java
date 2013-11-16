package conversations.usertaxi.actions;

import messages.DestinationReachedMessage;
import util.protocols.*;

public abstract class DestinationReachedAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((DestinationReachedMessage)event);
	}
	
	public abstract void processMessage(DestinationReachedMessage msg);
}