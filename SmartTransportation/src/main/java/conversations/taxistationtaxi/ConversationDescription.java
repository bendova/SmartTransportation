package conversations.taxiStationTaxi;

import conversations.taxiStationTaxi.messages.*;
import util.protocols.*;

public class ConversationDescription extends FSMDescription
{
	public static final String PROTOCOL_NAME = "TaxiWithTaxiStationConversationProtocol";
	
	public enum States
	{
		WAITING_REGISTRATION,
		WAITING_ORDER,
		COMPLETING_ORDER,
		IN_REVISION,
		RETIRED
	}
	
	public enum Transitions
	{
		REGISTERING,
		SENDING_ORDER,
		ORDER_COMPLETE,
		REJECT_ORDER,
		GOING_TO_REVISION,
		REVISION_COMPLETE,
		RETIRING
	}
	
	public void init(	Action onRegisterAction,
						Action onSendOrderAction,
						Action onOrderCompleteAction,
						Action onRejectOrderAction,
						Action onGoingToRevisionAction,
						Action onRevisionCompleteAction) 
	{
		assert(onRegisterAction != null);
		assert(onSendOrderAction != null);
		assert(onOrderCompleteAction != null);
		assert(onRejectOrderAction != null);
		assert(onGoingToRevisionAction != null);
		assert(onRevisionCompleteAction != null);
		
		try
		{
			addState(States.WAITING_REGISTRATION,	StateType.START);
			addState(States.WAITING_ORDER, 			StateType.ACTIVE);
			addState(States.COMPLETING_ORDER, 		StateType.ACTIVE);
			addState(States.IN_REVISION, 			StateType.ACTIVE);
			addState(States.RETIRED, 				StateType.END);
			
			addTransition(Transitions.REGISTERING, new RegisterCondition(), 
					States.WAITING_REGISTRATION, States.WAITING_ORDER, onRegisterAction);
			
			addTransition(Transitions.SENDING_ORDER, new SendOrderCondition(), 
					States.WAITING_ORDER, States.COMPLETING_ORDER, onSendOrderAction);
			
			addTransition(Transitions.ORDER_COMPLETE, new CompletingOrderCondition(), 
					States.COMPLETING_ORDER, States.WAITING_ORDER, onOrderCompleteAction);
			
			addTransition(Transitions.REJECT_ORDER, new RejectOrderCondition(), 
					States.COMPLETING_ORDER, States.IN_REVISION, onRejectOrderAction);
			
			addTransition(Transitions.GOING_TO_REVISION, new GoToRevisionCondition(), 
					States.WAITING_ORDER, States.IN_REVISION, onGoingToRevisionAction);
			
			addTransition(Transitions.REVISION_COMPLETE, new RevisionCompleteCondition(), 
					States.IN_REVISION, States.WAITING_ORDER, onRevisionCompleteAction);
			
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
			if(event instanceof RegisterAsTaxiMessage)
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
			if(event instanceof TaxiOrderMessage)
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
			if(event instanceof TaxiOrderCompleteMessage)
			{
				return true;
			}
			return false;
		}
	}
	
	private class RejectOrderCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if(event instanceof RejectOrderMessage)
			{
				return true;
			}
			return false;
		}
	}
	
	private class GoToRevisionCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if(event instanceof TaxiStatusUpdateMessage)
			{
				return true;
			}
			return false;
		}
	}
	
	private class RevisionCompleteCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) {
			if(event instanceof TaxiStatusUpdateMessage)
			{
				return true;
			}
			return false;
		}
	}
}
