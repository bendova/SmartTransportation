package conversations.userTaxi.actions;

import conversations.userTaxi.messages.RequestDestinationMessage;
import util.protocols.*;

public abstract class RequestDestinationAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((RequestDestinationMessage)event);
	}
	
	public abstract void processMessage(RequestDestinationMessage msg);
}
