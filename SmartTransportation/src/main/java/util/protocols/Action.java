package util.protocols;

import util.protocols.Transition;

/**
 * An FSM Action is something which is executed when a transition is taken.
 * 
 * @author Sam Macbeth
 * 
 */
public interface Action {

	/**
	 * A no-op Action which does nothing.
	 */
	public static final Action NOOP = new Action() {
		@Override
		public void execute(Object event, Object entity, Transition transition) {
		}
	};

	/**
	 * Executes this action for the {@link Transition} <code>transition</code>
	 * caused by <code>event</code>.
	 * 
	 * @param event
	 *            the event which caused this transition.
	 * @param entity
	 *            Entity object for this FSM.
	 * @param transition
	 *            transition being taken.
	 */
	public void execute(Object event, Object entity, Transition transition);

}

