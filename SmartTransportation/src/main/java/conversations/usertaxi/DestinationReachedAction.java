package conversations.usertaxi;

import uk.ac.imperial.presage2.util.fsm.*;
import messages.DestinationReachedMessage;

public abstract class DestinationReachedAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((DestinationReachedMessage)event);
	}
	
	public abstract void processMessage(DestinationReachedMessage msg);
}