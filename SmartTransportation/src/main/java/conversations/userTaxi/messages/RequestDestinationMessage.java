package conversations.userTaxi.messages;

import uk.ac.imperial.presage2.core.messaging.Performative;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import uk.ac.imperial.presage2.util.protocols.Conversation;
import util.TimeStamp;

public class RequestDestinationMessage extends UnicastMessage<String>
{
	private Conversation mConversation;
	
	public RequestDestinationMessage(NetworkAddress from, NetworkAddress to)
	{
		super(Performative.REQUEST, from, to, new TimeStamp());
		
		data = "Where do you want to go, sir?";
	}
	
	public void setConversation(Conversation conversation)
	{
		assert(conversation != null);
		
		mConversation = conversation;
	}
	
	public Conversation getConversation()
	{
		assert(mConversation != null);
		
		return mConversation;
	}
}
