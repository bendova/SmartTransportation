package messages.messageData;

import java.util.UUID;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public class TaxiOrder 
{
	private UUID mUserID;
	private UUID mUserAuthkey;
	private Location mLocation;
	private NetworkAddress mUserNetworkAddress;
	
	public TaxiOrder(Location location, UUID userID, UUID userAuthkey, NetworkAddress userNetworkAddress)
	{
		assert(userID != null);
		assert(location != null);
		assert(userNetworkAddress != null);
		
		mUserID = userID;
		mLocation = location;
		mUserAuthkey = userAuthkey;
		mUserNetworkAddress = userNetworkAddress;
	}
	
	public UUID getUserID() 
	{
		return mUserID;
	}
	
	public UUID getUserAuthKey()
	{
		return mUserAuthkey;
	}
	
	public Location getUserLocation()
	{
		return mLocation;
	}
	
	public NetworkAddress getUserNetworkAddress()
	{
		return mUserNetworkAddress;
	}
}
