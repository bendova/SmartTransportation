package conversations.usertaxi;

import protocols.FSMConversation;
import uk.ac.imperial.presage2.core.network.Message;
import uk.ac.imperial.presage2.util.fsm.*;

public class SendMessage implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		((FSMConversation)entity).getNetwork().sendMessage((Message<?>)event);
	}
	
}