package util.protocols;

import java.util.Iterator;
import java.util.Set;

import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.network.Message;
import uk.ac.imperial.presage2.core.network.NetworkAdaptor;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.simulator.SimTime;
import uk.ac.imperial.presage2.util.protocols.Conversation;
import uk.ac.imperial.presage2.util.protocols.Protocol;
import uk.ac.imperial.presage2.util.protocols.Role;

public class FSMProtocol extends Protocol implements TimeDriven {

	protected final FSMDescription description;
	protected final NetworkAdaptor network;

	public FSMProtocol(String name, FSMDescription description, NetworkAdaptor network) {
		super(name);
		this.description = description;
		this.network = network;
	}

	@Override
	public Conversation spawn() {
		try {
			return spawnAsInititor();
		} catch (FSMException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Conversation spawn(NetworkAddress with) {
		try {
			return spawnAsInititor();
		} catch (FSMException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Conversation spawn(Set<NetworkAddress> with) {
		try {
			return spawnAsInititor();
		} catch (FSMException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void spawn(Message<?> m) {
		FSMConversation conv = new FSMConversation(description, this.name, Role.REPLIER, network);
		conv.setConversationKey(m.getConversationKey());
		conv.recipients.add(m.getFrom());
		try {
			activeConversations.add(conv);
			conv.fsm.applyEvent(m);
		} catch (FSMException e) {
			// TODO log this
		}
	}

	protected Conversation spawnAsInititor() throws FSMException {
		FSMConversation conv = new FSMConversation(description, this.name, Role.INITIATOR, network);
		this.activeConversations.add(conv);
		return conv;
	}

	@Override
	public void incrementTime() {
		Timeout t = new Timeout(SimTime.get().intValue());
		for (Iterator<Conversation> it = activeConversations.iterator(); it.hasNext();) {
			FSMConversation c = (FSMConversation) it.next();
			if (c.fsm.canApplyEvent(t)) {
				try {
					c.fsm.applyEvent(t);
				} catch (FSMException e) {
				}
			}
			if (c.isFinished())
				it.remove();
		}
	}

}

