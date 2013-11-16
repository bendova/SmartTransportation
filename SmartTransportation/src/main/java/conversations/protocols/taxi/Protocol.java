package conversations.protocols.taxi;

import conversations.usertaxi.actions.TakeMeToDestinationAction;
import uk.ac.imperial.presage2.core.network.*;

public class Protocol 
{
	private NetworkAdaptor mNetworkAdaptor;
	private ProtocolWithUser mWithUser;
	
	public Protocol(NetworkAdaptor network)
	{
		mNetworkAdaptor = network;
	}
	
	public void initProtocolWithUser(TakeMeToDestinationAction takeMeToDestinationAction)
	{
		assert(mWithUser == null);
		
		mWithUser = new ProtocolWithUser(mNetworkAdaptor);
		mWithUser.init(takeMeToDestinationAction);
	}
	
	public ProtocolWithUser withUser() 
	{
		return mWithUser;
	}
}
