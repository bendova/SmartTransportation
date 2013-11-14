package conversations.usertaxi;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import protocols.FSMProtocol;

import messages.DestinationReachedMessage;
import messages.RequestDestinationMessage;
import messages.TakeMeToDestinationMessage;

import uk.ac.imperial.presage2.core.network.*;
import uk.ac.imperial.presage2.util.fsm.*;
import uk.ac.imperial.presage2.util.protocols.Conversation;

public class TaxiProtocol 
{
	private NetworkAdaptor mNetworkAdaptor;
	private FSMProtocol mWithUserProtocol;
	private Map<NetworkAddress, UUID> conversationsMap = 
			new HashMap<NetworkAddress, UUID>();
	
	public TaxiProtocol(NetworkAdaptor network)
	{
		mNetworkAdaptor = network;
	}
	
	public void initProtocolWithUser(TakeMeToDestinationAction takeMeToDestinationAction)
	{
		assert(mWithUserProtocol == null);
		
		ConversationDescription desc = new ConversationDescription();
		Action requestDestinationAction = new SendMessage();
		Action destinationReachedAction = new SendMessage();
		desc.init(requestDestinationAction, takeMeToDestinationAction, 
				destinationReachedAction);
		
		mWithUserProtocol = new FSMProtocol(ConversationDescription.PROTOCOL_NAME, 
				desc, mNetworkAdaptor);
	}
	
	public void handle(RequestDestinationMessage msg)
	{
		Conversation conversation = mWithUserProtocol.spawn(msg.getFrom());
		conversationsMap.put(msg.getTo(), conversation.getID());
//		msg.setConversation(conversation);
		handleMessage(msg);
	}
	
	public void handle(TakeMeToDestinationMessage msg)
	{
		handleMessage(msg);
	}
	
	public void handle(DestinationReachedMessage msg)
	{
		handleMessage(msg);
	}
	
	private void handleMessage(UnicastMessage<?> msg)
	{
		msg.setConversationKey(conversationsMap.get(msg.getTo()));
		msg.setProtocol(ConversationDescription.PROTOCOL_NAME);
		mWithUserProtocol.handle(msg);
	}
}
