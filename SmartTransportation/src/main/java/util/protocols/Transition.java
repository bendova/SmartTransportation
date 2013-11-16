package util.protocols;

import util.protocols.Action;
import util.protocols.State;
import util.protocols.TransitionCondition;

public class Transition {

	private final String name;
	private final State start;
	private final State end;
	private TransitionCondition condition;
	private Action action;

	/**
	 * @param name
	 * @param start
	 * @param end
	 * @param condition
	 * @param action
	 */
	Transition(String name, State start, State end, TransitionCondition condition, Action action) {
		super();
		this.name = name;
		this.start = start;
		this.end = end;
		this.condition = condition;
		this.action = action;
	}

	public String getName() {
		return name;
	}

	State getStart() {
		return start;
	}

	State getEnd() {
		return end;
	}

	TransitionCondition getCondition() {
		return condition;
	}

	Action getAction() {
		return action;
	}

	void setCondition(TransitionCondition condition) {
		this.condition = condition;
	}

	void setAction(Action action) {
		this.action = action;
	}
}

