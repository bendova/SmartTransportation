package conversations.userTaxi.actions;

import conversations.userTaxi.messages.TakeMeToDestinationMessage;
import util.protocols.*;

public abstract class TakeMeToDestinationAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition) 
	{
		processMessage((TakeMeToDestinationMessage)event);
	}
	
	public abstract void processMessage(TakeMeToDestinationMessage msg);
}
