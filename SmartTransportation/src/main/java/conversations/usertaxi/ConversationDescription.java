package conversations.usertaxi;

import messages.DestinationReachedMessage;
import messages.RequestDestinationMessage;
import messages.TakeMeToDestinationMessage;
import uk.ac.imperial.presage2.util.fsm.*;

public class ConversationDescription extends FSMDescription
{
	public static final String PROTOCOL_NAME = "UserWithTaxiConvesationProtocol";
	
	private static final String STATE_WAITING_REQUEST 			= "WaitingRequestDestination";
	private static final String STATE_REQUEST_DESTINATION 		= "RequestDestination";
	private static final String STATE_TAKE_ME_TO_DESTINATION	= "TakeMeToDestination";
	private static final String STATE_DESTINATION_REACHED 		= "DestinationReached";
	
	public void init(	Action requestDestinationAction,
						Action takeMeToDestinationAction,
						Action destinationReachedAction) 
	{
		assert(requestDestinationAction != null);
		assert(takeMeToDestinationAction != null);
		assert(destinationReachedAction != null);
		
		try
		{
			addState(STATE_WAITING_REQUEST, 		StateType.START);
			addState(STATE_REQUEST_DESTINATION, 	StateType.ACTIVE);
			addState(STATE_TAKE_ME_TO_DESTINATION, 	StateType.ACTIVE);
			addState(STATE_DESTINATION_REACHED, 	StateType.END);
			
			RequestDestinationCondition reqDestCond = new RequestDestinationCondition();
			addTransition("RequestingDestination", reqDestCond, 
					STATE_WAITING_REQUEST, STATE_REQUEST_DESTINATION, requestDestinationAction);
			
			TakeMeToDestinationCondition takeMeToDestCond = new TakeMeToDestinationCondition();
			addTransition("SendingDestination", takeMeToDestCond, 
					STATE_REQUEST_DESTINATION, STATE_TAKE_ME_TO_DESTINATION, takeMeToDestinationAction);
			
			DestinationReachedCondition destReachedCond = new DestinationReachedCondition();
			addTransition("DestinationReached", destReachedCond, 
					STATE_TAKE_ME_TO_DESTINATION, STATE_DESTINATION_REACHED, destinationReachedAction);
			this.build();
		}
		catch(FSMException e)
		{
			e.printStackTrace();
		}
	}
	
	private class RequestDestinationCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) 
		{
			if((state.getName() == ConversationDescription.STATE_WAITING_REQUEST) &&
				(event instanceof RequestDestinationMessage))
			{
				return true;
			}
			return false;
		}	
	}
	
	private class TakeMeToDestinationCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) 
		{
			if((state.getName() == ConversationDescription.STATE_REQUEST_DESTINATION) &&
				(event instanceof TakeMeToDestinationMessage))
			{
				return true;
			}
			return false;
		}
	}
	
	private class DestinationReachedCondition implements TransitionCondition
	{
		@Override
		public boolean allow(Object event, Object entity, State state) 
		{
			if((state.getName() == ConversationDescription.STATE_TAKE_ME_TO_DESTINATION) &&
				(event instanceof DestinationReachedMessage))
			{
				return true;
			}
			return false;
		}
	}
	
}
