package conversations.protocols.user;

import java.util.UUID;

import uk.ac.imperial.presage2.core.network.NetworkAdaptor;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.protocols.FSMProtocol;
import conversations.commonActions.SendMessageAction;
import conversations.userTaxi.ConversationDescription;
import conversations.userTaxi.actions.DestinationReachedAction;
import conversations.userTaxi.actions.RequestDestinationAction;
import conversations.userTaxi.messages.DestinationReachedMessage;
import conversations.userTaxi.messages.RequestDestinationMessage;
import conversations.userTaxi.messages.TakeMeToDestinationMessage;

public class ProtocolWithTaxi 
{
	private NetworkAdaptor mNetworkAdaptor;
	private FSMProtocol mWithTaxiProtocol;
	private UUID mConversationKey;
	
	public ProtocolWithTaxi(NetworkAdaptor adaptor)
	{
		mNetworkAdaptor = adaptor;
	}
	
	public void init(RequestDestinationAction requestDestinationAction,
			DestinationReachedAction destinationReachedAction)
	{
		assert(mWithTaxiProtocol == null);
		
		ConversationDescription desc = new ConversationDescription();
		SendMessageAction takeMeToDestinationAction = new SendMessageAction();
		desc.init(requestDestinationAction, takeMeToDestinationAction, 
				destinationReachedAction);
		
		mWithTaxiProtocol = new FSMProtocol(ConversationDescription.PROTOCOL_NAME, 
				desc, mNetworkAdaptor);
	}
	
	public void handleRequestDestination(RequestDestinationMessage msg)
	{
		mConversationKey = msg.getConversationKey();
		mWithTaxiProtocol.spawn(msg);
	}
	
	public void sendTakeMeToDestination(TakeMeToDestinationMessage msg)
	{
		sendMessage(msg);
	}
	
	public void handleOnDestinationReached(DestinationReachedMessage msg)
	{
		mWithTaxiProtocol.handle(msg);
	}
	
	private void sendMessage(UnicastMessage<?> msg)
	{
		msg.setConversationKey(mConversationKey);
		msg.setProtocol(ConversationDescription.PROTOCOL_NAME);
		mWithTaxiProtocol.handle(msg);
	}
}
