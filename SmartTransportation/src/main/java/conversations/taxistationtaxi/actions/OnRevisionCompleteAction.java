package conversations.taxiStationTaxi.actions;

import conversations.taxiStationTaxi.messages.RevisionCompleteMessage;
import util.protocols.Action;
import util.protocols.Transition;

public abstract class OnRevisionCompleteAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((RevisionCompleteMessage)event);
	}
	
	public abstract void processMessage(RevisionCompleteMessage msg);
}
