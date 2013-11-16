package conversations.taxistationtaxi;

import messages.RegisterAsTaxiMessage;
import messages.RevisionCompleteMessage;
import messages.TaxiOrderCompleteMessage;
import messages.TaxiOrderMessage;
import messages.TaxiStatusUpdateMessage;
import util.protocols.*;

public class ConversationDescription extends FSMDescription
{
	public static final String PROTOCOL_NAME = "TaxiWithTaxiStationConvesationProtocol";
	
	private static final String STATE_WAITING_REGISTRATION 	= "WaitingRegistration";
	private static final String STATE_WAITING_ORDER 		= "WaitingOrder";
	private static final String STATE_COMPLETING_ORDER 		= "CompletingOrder";
	private static final String STATE_UNAVAILABLE 			= "Unavailable";
	private static final String STATE_RETIRED 				= "Retired";
	
	public void init(	Action requestDestinationAction,
						Action takeMeToDestinationAction,
						Action destinationReachedAction) 
	{
		assert(requestDestinationAction != null);
		assert(takeMeToDestinationAction != null);
		assert(destinationReachedAction != null);
		
		try
		{
			addState(STATE_WAITING_REGISTRATION,	StateType.START);
			addState(STATE_WAITING_ORDER, 			StateType.ACTIVE);
			addState(STATE_COMPLETING_ORDER, 		StateType.ACTIVE);
			addState(STATE_UNAVAILABLE, 			StateType.ACTIVE);
			addState(STATE_RETIRED, 				StateType.END);
			
			RegisterCondition regCondition = new RegisterCondition();
			addTransition("Registering", regCondition, STATE_WAITING_REGISTRATION, STATE_WAITING_ORDER, Action.NOOP);
			
			SendOrderCondition sendOrderCondition = new SendOrderCondition();
			addTransition("SendingOrder", sendOrderCondition, STATE_WAITING_ORDER, STATE_COMPLETING_ORDER, Action.NOOP);
			
			CompletingOrderCondition completingOrderCondition = new CompletingOrderCondition();
			addTransition("CompletingOrder", completingOrderCondition, STATE_COMPLETING_ORDER, STATE_WAITING_ORDER, Action.NOOP);
			
			GoToUnavailableCondition unavailableCondition = new GoToUnavailableCondition();
			addTransition("GoToUnavailable", unavailableCondition, STATE_WAITING_ORDER, STATE_UNAVAILABLE, Action.NOOP);
			
			GoToBackToWorkCondition goToWorkCondition = new GoToBackToWorkCondition();
			addTransition("GoBackToWork", goToWorkCondition, STATE_UNAVAILABLE, STATE_WAITING_ORDER, Action.NOOP);
			
			this.build();
		}
		catch(FSMException e)
		{
			e.printStackTrace();
		}
	}
	
	private class RegisterCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if((state.getName() == ConversationDescription.STATE_WAITING_REGISTRATION) &&
				(event instanceof RegisterAsTaxiMessage))
			{
				return true;
			}
			return false;
		}
	}
	
	private class SendOrderCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if((state.getName() == ConversationDescription.STATE_WAITING_ORDER) &&
				(event instanceof TaxiOrderMessage))
			{
				return true;
			}
			return false;
		}
	}
	
	private class CompletingOrderCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if((state.getName() == ConversationDescription.STATE_COMPLETING_ORDER) &&
				(event instanceof TaxiOrderCompleteMessage))
			{
				return true;
			}
			return false;
		}
	}
	
	private class GoToUnavailableCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if((state.getName() == ConversationDescription.STATE_WAITING_ORDER) &&
				(event instanceof TaxiStatusUpdateMessage))
			{
				return true;
			}
			return false;
		}
	}
	
	private class GoToBackToWorkCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if((state.getName() == ConversationDescription.STATE_UNAVAILABLE) &&
				(event instanceof RevisionCompleteMessage))
			{
				return true;
			}
			return false;
		}
	}
}
