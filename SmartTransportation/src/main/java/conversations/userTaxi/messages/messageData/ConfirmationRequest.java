package conversations.userTaxi.messages.messageData;

public class ConfirmationRequest implements IConfirmationRequest
{
	private double mTotalTravelTime;
	private double mTravelCost;
	public ConfirmationRequest(double totalTravelTime, double travelCost)
	{
		assert(totalTravelTime >= 0);
		assert(travelCost >= 0);
		
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
}
