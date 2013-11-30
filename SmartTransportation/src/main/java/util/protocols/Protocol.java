package util.protocols;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.messaging.InputHandler;
import uk.ac.imperial.presage2.core.network.Message;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.protocols.Conversation;

public abstract class Protocol implements InputHandler, TimeDriven
{
	protected final String name;

	protected List<Conversation> activeConversations = new CopyOnWriteArrayList<Conversation>();

	protected Protocol(String name) {
		this.name = name;
	}

	@Override
	public boolean canHandle(Input in) {
		if (in instanceof Message) {
			return ((Message<?>) in).getProtocol().equals(name);
		}
		return false;
	}

	@Override
	public void handle(Input in) {
		if (canHandle(in)) {
			// pass this input to all conversation that could handle it.
			int handleCount = 0;
			for (Iterator<Conversation> it = activeConversations.iterator(); it.hasNext();) 
			{
				Conversation c = it.next();
				if (c.canHandle(in)) 
				{
					c.handle(in);
					handleCount++;
				}
				if (c.isFinished())
				{
					activeConversations.remove(c);
//					it.remove();
				}
			}
			if (handleCount == 0) 
			{
				spawn((Message<?>) in);
			}
		}
	}

	/**
	 * Spawn a conversation of this protocol. No arguments are provided so this
	 * will usually be a broadcast conversation.
	 */
	public abstract Conversation spawn();

	/**
	 * Spawn a conversation of this protocol with the given network address.
	 * 
	 * @param with
	 */
	public abstract Conversation spawn(NetworkAddress with);

	/**
	 * Spawn a conversation of this protocol with the given network addresses
	 * (multi-cast conversation).
	 * 
	 * @param with
	 * @return
	 */
	public abstract Conversation spawn(Set<NetworkAddress> with);

	/**
	 * Spawn a conversation of this protocol from the given input message
	 * (reactive conversation).
	 * 
	 * @param m
	 * @return
	 */
	public abstract void spawn(Message<?> m);

	/**
	 * Get the set of active conversations managed by this protocol.
	 * 
	 * @return
	 */
	public Set<Conversation> getActiveConversations() {
		return Collections.unmodifiableSet(new HashSet<Conversation>(this.activeConversations));
	}

	/**
	 * Get the set of {@link NetworkAddress}es involved in an active
	 * conversation with us in this protocol.
	 * 
	 * @return
	 */
	public Set<NetworkAddress> getActiveConversationMembers() {
		Set<NetworkAddress> members = new HashSet<NetworkAddress>();
		for (Conversation c : getActiveConversations()) {
			members.addAll(c.getMembers());
		}
		return Collections.unmodifiableSet(members);
	}
}
