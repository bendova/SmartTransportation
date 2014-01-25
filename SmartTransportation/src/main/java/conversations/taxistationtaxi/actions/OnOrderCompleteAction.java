package conversations.taxiStationTaxi.actions;

import conversations.taxiStationTaxi.messages.TaxiOrderCompleteMessage;
import util.protocols.Action;
import util.protocols.Transition;

public abstract class OnOrderCompleteAction implements Action
{
	@Override
	public void execute(Object event, Object entity, Transition transition)
	{
		processMessage((TaxiOrderCompleteMessage)event);
	}
	
	public abstract void processMessage(TaxiOrderCompleteMessage msg);
}
