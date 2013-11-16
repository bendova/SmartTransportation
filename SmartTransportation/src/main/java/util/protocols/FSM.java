package util.protocols;

import java.util.HashSet;
import java.util.Set;

public class FSM {

	private final FSMDescription desc;
	private final Object entity;

	private State currentState;

	public FSM(FSMDescription desc, Object entity) {
		super();
		this.desc = desc;
		this.entity = entity;
		this.currentState = this.desc.getStartState();
	}

	public static FSMDescription description() {
		return new FSMDescription();
	}

	public String getState() {
		return currentState.getName();
	}

	public Object getEntity() {
		return entity;
	}

	public boolean canApplyEvent(Object event) {
		for (Transition t : this.currentState.getTransitions()) {
			if (t.getCondition().allow(event, entity, currentState)) {
				return true;
			}
		}
		return false;
	}

	public void applyEvent(Object event) throws FSMException {
		Set<Transition> possible = new HashSet<Transition>();
		for (Transition t : this.currentState.getTransitions()) {
			if (t.getCondition().allow(event, entity, currentState)) {
				possible.add(t);
			}
		}
		int count = possible.size();
		if (count > 1) {
			// multiple paths, pick arbitrarily.
			// TODO more deterministic behaviour
			for (Transition t : possible) {
				doTransition(event, t);
				break;
			}
		} else if (count == 1) {
			for (Transition t : possible) {
				doTransition(event, t);
				break;
			}
		} else {
			// no available transition, throw exception
			throw new FSMException("No transition available from this state with this event.");
		}
	}

	private void doTransition(Object e, Transition t) {
		this.currentState = t.getEnd();
		t.getAction().execute(e, entity, t);
	}

	public boolean isStartState() {
		return currentState.getType() == StateType.START;
	}

	public boolean isEndState() {
		return currentState.getType() == StateType.END;
	}
}

