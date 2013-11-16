package conversations.protocols.user;

import uk.ac.imperial.presage2.core.network.NetworkAdaptor;

public class ProtocolWithMediator 
{
	private NetworkAdaptor mAdaptor;
	
	public ProtocolWithMediator(NetworkAdaptor adaptor)
	{
		mAdaptor = adaptor;
	}
	
	public void init()
	{
		// TODO
	}
}
