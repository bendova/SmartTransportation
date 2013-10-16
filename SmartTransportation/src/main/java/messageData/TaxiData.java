package messageData;

import java.util.UUID;

import uk.ac.imperial.presage2.core.network.NetworkAddress;

public class TaxiData 
{
	private UUID mTaxiID;
	private NetworkAddress mTaxiNetworkAddress;
	
	public TaxiData(UUID taxiID, NetworkAddress taxiNetworkAddress)
	{
		assert(taxiID != null);
		assert(taxiNetworkAddress != null);
		
		mTaxiID = taxiID;
		mTaxiNetworkAddress = taxiNetworkAddress;
	}
	
	public UUID getID() 
	{
		return mTaxiID;
	}
	
	public NetworkAddress getNetworkAddress()
	{
		return mTaxiNetworkAddress;
	}
	
	@Override
	public int hashCode()
	{
		return mTaxiID.hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if((obj != null) && (obj instanceof TaxiData))
		{
			return (mTaxiID == ((TaxiData)obj).getID());
		}
		return false;
	}
}
