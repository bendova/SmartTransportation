package conversations.usertaxi;

import messages.TakeMeToDestinationMessage;
import uk.ac.imperial.presage2.util.fsm.*;

public abstract class TakeMeToDestinationAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition) 
	{
		processMessage((TakeMeToDestinationMessage)event);
	}
	
	public abstract void processMessage(TakeMeToDestinationMessage msg);
}
