package conversations.protocols.taxi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import messages.DestinationReachedMessage;
import messages.RequestDestinationMessage;
import messages.TakeMeToDestinationMessage;
import conversations.commonActions.SendMessageAction;
import conversations.usertaxi.ConversationDescription;
import conversations.usertaxi.actions.TakeMeToDestinationAction;
import uk.ac.imperial.presage2.core.network.NetworkAdaptor;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import uk.ac.imperial.presage2.util.protocols.Conversation;
import util.protocols.Action;
import util.protocols.FSMProtocol;

public class ProtocolWithUser 
{
	private NetworkAdaptor mNetworkAdaptor;
	private FSMProtocol mWithUserProtocol;
	private Map<NetworkAddress, UUID> conversationsMap = 
			new HashMap<NetworkAddress, UUID>();

	public ProtocolWithUser(NetworkAdaptor network)
	{
		mNetworkAdaptor = network;
	}
	
	public void init(TakeMeToDestinationAction takeMeToDestinationAction)
	{
		assert(mWithUserProtocol == null);
		
		ConversationDescription desc = new ConversationDescription();
		Action requestDestinationAction = new SendMessageAction();
		Action destinationReachedAction = new SendMessageAction();
		desc.init(requestDestinationAction, takeMeToDestinationAction, 
				destinationReachedAction);
		
		mWithUserProtocol = new FSMProtocol(ConversationDescription.PROTOCOL_NAME, 
				desc, mNetworkAdaptor);
	}
	
	public void requestDestination(RequestDestinationMessage msg)
	{
		Conversation conversation = mWithUserProtocol.spawn(msg.getFrom());
		conversationsMap.put(msg.getTo(), conversation.getID());
		msg.setConversationKey(conversation.getID());
		handleMessage(msg);
	}
	
	public void handleTakeMeToDestination(TakeMeToDestinationMessage msg)
	{
		msg.setConversationKey(conversationsMap.get(msg.getFrom()));
		handleMessage(msg);
	}
	
	public void reportDestinationReached(DestinationReachedMessage msg)
	{
		NetworkAddress userAddress = msg.getTo();
		msg.setConversationKey(conversationsMap.get(userAddress));
		handleMessage(msg);
		conversationsMap.remove(userAddress); // conversation complete
	}
	
	private void handleMessage(UnicastMessage<?> msg)
	{
		msg.setProtocol(ConversationDescription.PROTOCOL_NAME);
		mWithUserProtocol.handle(msg);
	}
}
