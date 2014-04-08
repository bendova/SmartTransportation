package conversations.taxiStationMediator.messageData;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public class TaxiDescription implements ITaxiDescription
{
	private NetworkAddress mTaxiAddress;
	private NetworkAddress mTaxiStationAddress;
	private Location mTaxiLocation;
	public TaxiDescription(NetworkAddress taxiAddress, Location taxiLocation, 
			NetworkAddress taxiStationAddress)
	{
		assert(taxiAddress != null);
		assert(taxiLocation != null);
		assert(taxiStationAddress != null);
		
		mTaxiAddress = taxiAddress;
		mTaxiLocation = taxiLocation;
		mTaxiStationAddress = taxiStationAddress;
	}

	@Override
	public NetworkAddress getTaxiStationAddress()
	{
		return mTaxiStationAddress;
	}
	
	@Override
	public NetworkAddress getTaxiAddress() 
	{
		return mTaxiAddress;
	}

	@Override
	public Location getTaxiLocation() 
	{
		return mTaxiLocation;
	}
}
