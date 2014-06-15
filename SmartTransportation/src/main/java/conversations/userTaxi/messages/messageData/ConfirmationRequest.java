package conversations.userTaxi.messages.messageData;

import uk.ac.imperial.presage2.core.network.NetworkAddress;

public class ConfirmationRequest implements IConfirmationRequest
{
	private NetworkAddress mUserAddress;
	private double mTotalTravelTime;
	private double mTravelCost;
	public ConfirmationRequest(NetworkAddress userAddress, double totalTravelTime, double travelCost)
	{
		assert(userAddress != null);
		assert(totalTravelTime >= 0);
		assert(travelCost >= 0);
		
		mUserAddress = userAddress;
		mTotalTravelTime = totalTravelTime;
		mTravelCost = travelCost;
	}
	
	@Override
	public double getTotalTravelTime()
	{
		return mTotalTravelTime;
	}
	@Override
	public double getTravelCost()
	{
		return mTravelCost;
	}

	@Override
	public NetworkAddress getUserAddress() 
	{
		return mUserAddress;
	}
}
