package util.protocols;

import util.protocols.State;
import util.protocols.TransitionCondition;

/**
 * Decides whether a transition may be made from a state given an event.
 * 
 * @author Sam Macbeth
 * 
 */
public interface TransitionCondition {

	public static final TransitionCondition ALWAYS = new TransitionCondition() {
		@Override
		public boolean allow(Object event, Object entity, State state) {
			return true;
		}
	};

	/**
	 * Test whether this state transition is allowed given the event object and
	 * current state.
	 * 
	 * @param event
	 *            The event which is being applied on the state machine.
	 * @param entity
	 *            Entity associated with this state machine.
	 * @param state
	 *            Current state of the FSM.
	 * @return true if this transition is allowed from <code>state</code> given
	 *         <code>event</code>
	 */
	public boolean allow(Object event, Object entity, State state);

}

