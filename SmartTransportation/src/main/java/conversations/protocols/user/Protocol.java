package conversations.protocols.user;

import conversations.usertaxi.actions.DestinationReachedAction;
import conversations.usertaxi.actions.RequestDestinationAction;
import uk.ac.imperial.presage2.core.network.NetworkAdaptor;

public class Protocol
{	
	private NetworkAdaptor mNetworkAdaptor;
	private ProtocolWithTaxi mWithTaxi;
	
	public Protocol(NetworkAdaptor network)
	{
		mNetworkAdaptor = network;
	}
	
	public void initProtocolWithTaxi(RequestDestinationAction requestDestinationAction,
			DestinationReachedAction destinationReachedAction)
	{
		assert(mWithTaxi == null);
		
		mWithTaxi = new ProtocolWithTaxi(mNetworkAdaptor);
		mWithTaxi.init(requestDestinationAction, destinationReachedAction);
	}
	
	public ProtocolWithTaxi withTaxi()
	{
		assert(mWithTaxi != null);
		
		return mWithTaxi;
	}
}
