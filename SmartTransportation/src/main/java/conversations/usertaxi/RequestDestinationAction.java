package conversations.usertaxi;

import messages.RequestDestinationMessage;
import uk.ac.imperial.presage2.util.fsm.*;

public abstract class RequestDestinationAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((RequestDestinationMessage)event);
	}
	
	public abstract void processMessage(RequestDestinationMessage msg);
}
