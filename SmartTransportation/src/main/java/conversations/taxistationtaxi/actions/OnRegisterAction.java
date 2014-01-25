package conversations.taxiStationTaxi.actions;

import conversations.taxiStationTaxi.messages.RegisterAsTaxiMessage;
import util.protocols.Action;
import util.protocols.Transition;

public abstract class OnRegisterAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((RegisterAsTaxiMessage)event);
	}
	
	public abstract void processMessage(RegisterAsTaxiMessage msg);
}