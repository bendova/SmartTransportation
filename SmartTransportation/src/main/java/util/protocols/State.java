package util.protocols;

import java.util.HashSet;
import java.util.Set;

public class State {

	private final String name;
	private final StateType type;

	private final Set<Transition> transitions = new HashSet<Transition>();

	State(String name, StateType type) {
		super();
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public StateType getType() {
		return type;
	}

	void addTransition(Transition trans) {
		transitions.add(trans);
	}

	Set<Transition> getTransitions() {
		return transitions;
	}
}

