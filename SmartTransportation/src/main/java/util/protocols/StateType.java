package util.protocols;

/**
 * Defines the different types a state may be.
 * 
 * @author Sam Macbeth
 * 
 */
public enum StateType {
	/**
	 * The initial state of the FSM. May only have one per state machine.
	 */
	START,
	/**
	 * Any state which is not a start or end state of the FSM.
	 */
	ACTIVE,
	/**
	 * An end state of the fsm. No transitions can be made out of this state.
	 */
	END
}
