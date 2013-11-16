package conversations.usertaxi.actions;

import messages.TakeMeToDestinationMessage;
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
