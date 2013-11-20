package conversations.commonActions;

import uk.ac.imperial.presage2.core.network.Message;
import util.protocols.*;

public class SendMessageAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		((FSMConversation)entity).getNetwork().sendMessage((Message<?>)event);
	}
	
}