package conversations.taxiStationMediator.messageData;

import java.util.UUID;

import conversations.userMediator.messages.messageData.ITransportServiceRequest;

import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;

public class TaxiServiceRequest implements ITaxiServiceRequest
{
	private UUID mUserID;
	private UUID mUserAuthKey;
	private NetworkAddress mUserAddress;
	private Location mUserLocation;
	private ITaxiDescription mTaxiDescription;
	
	public TaxiServiceRequest(ITransportServiceRequest request, ITaxiDescription taxiDescription)
	{
		assert(request != null);
		assert(taxiDescription != null);
		
		mUserID = request.getUserID();
		mUserAuthKey = request.getUserAuthKey();
		mUserAddress = request.getUserNetworkAddress();
		mUserLocation = request.getStartLocation();
		mTaxiDescription = taxiDescription;
	}

	@Override
	public UUID getUserID() 
	{
		return mUserID;
	}
	@Override
	public UUID getUserAuthKey() 
	{
		return mUserAuthKey;
	}
	@Override
	public NetworkAddress getUserNetworkAddress() 
	{
		return mUserAddress;
	}
	@Override
	public Location getStartLocation() 
	{
		return mUserLocation;
	}
	@Override
	public ITaxiDescription getTaxiDescription()
	{
		return mTaxiDescription;
	}
}
