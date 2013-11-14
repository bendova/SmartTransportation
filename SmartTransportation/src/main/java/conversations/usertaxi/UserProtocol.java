package conversations.usertaxi;

import java.util.UUID;

import protocols.FSMProtocol;

import messages.DestinationReachedMessage;
import messages.RequestDestinationMessage;
import messages.TakeMeToDestinationMessage;
import uk.ac.imperial.presage2.core.network.NetworkAdaptor;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import uk.ac.imperial.presage2.util.fsm.Action;

public class UserProtocol
{	
	private NetworkAdaptor mNetworkAdaptor;
	private FSMProtocol mWithTaxiProtocol;
	private UUID mConversationKey;
	
	public UserProtocol(NetworkAdaptor network)
	{
		mNetworkAdaptor = network;
	}
	
	public void initProtocolWithTaxi(RequestDestinationAction requestDestinationAction,
			DestinationReachedAction destinationReachedAction)
	{
		assert(mWithTaxiProtocol == null);
		
		ConversationDescription desc = new ConversationDescription();
		Action takeMeToDestinationAction = new SendMessage();
		desc.init(requestDestinationAction, takeMeToDestinationAction, 
				destinationReachedAction);
		
		mWithTaxiProtocol = new FSMProtocol(ConversationDescription.PROTOCOL_NAME, 
				desc, mNetworkAdaptor);
	}
	
	public void handle(RequestDestinationMessage msg)
	{
		mConversationKey = msg.getConversationKey();
		mWithTaxiProtocol.spawn(msg);
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
		msg.setConversationKey(mConversationKey);
		msg.setProtocol(ConversationDescription.PROTOCOL_NAME);
		mWithTaxiProtocol.handle(msg);
	}
	
}
