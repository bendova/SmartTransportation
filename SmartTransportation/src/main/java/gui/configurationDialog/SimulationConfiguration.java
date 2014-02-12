package gui.configurationDialog;

public class SimulationConfiguration 
{
	private int mTimeStepDuration;
	private int mTimeStepsCount;
	private int mAreaSize;
	private int mPixelsPerAreaPoint;
	private int mUsersCount;
	private int mTaxiStationsCount;
	private int mTaxiesCount;
	private int mBusRoutesCount;
	private int mBusesCount;
	private int mTransportAllocationIndex;
	private int mTimeConstraintIndex;
	private boolean mIsWalkingEnabled;
	private boolean mAreTaxiesEnabled;
	private boolean mAreBusesEnabled;
	
	public SimulationConfiguration(int duration, int timeStepsCount, int areaSize, int pixelsPerAreaPoint, 
			int usersCount, boolean areTaxiesEnabled, int taxiesCount, int taxiStationsCount, boolean areBusesEnabled, 
			int busesCount, int busRoutesCount, boolean isWalkingEnabled, int transportAllocationIndex, int timeConstraintIndex)
	{
		assert(duration >= 0);
		assert(timeStepsCount >= 0);
		assert(areaSize >= 0);
		assert(pixelsPerAreaPoint >= 0);
		assert(taxiesCount >= 0);
		assert(usersCount >= 0);
		assert(taxiStationsCount >= 0);
		assert(busesCount >= 0);
		assert(busRoutesCount >= 0);
		assert(transportAllocationIndex >= 0);
		assert(timeConstraintIndex >= 0);
		
		mTimeStepDuration = duration;
		mTimeStepsCount = timeStepsCount;
		mAreaSize = areaSize;
		mPixelsPerAreaPoint = pixelsPerAreaPoint;
		mUsersCount = usersCount;
		mAreTaxiesEnabled = areTaxiesEnabled;
		mTaxiesCount = taxiesCount;
		mTaxiStationsCount = taxiStationsCount;
		mAreBusesEnabled = areBusesEnabled;
		mBusesCount = busesCount;
		mBusRoutesCount = busRoutesCount;
		mIsWalkingEnabled = isWalkingEnabled;
		mTransportAllocationIndex = transportAllocationIndex;
		mTimeConstraintIndex = timeConstraintIndex;
	}
	
	public int getTimeStepDuration()
	{
		return mTimeStepDuration;
	}
	
	public int getTimeStepsCount()
	{
		return mTimeStepsCount;
	}
	
	public int getAreaSize()
	{
		return mAreaSize;
	}
	
	public int getPixelsPerAreaPoint()
	{
		return mPixelsPerAreaPoint;
	}
	
	public int getUsersCount()
	{
		return mUsersCount;
	}
	
	public boolean isWalkingEnabled()
	{
		return mIsWalkingEnabled;
	}
	
	public boolean areTaxiesEnabled()
	{
		return mAreTaxiesEnabled;
	}
	
	public int getTaxiesCount()
	{
		return mTaxiesCount;
	}
	
	public int getTaxiStationsCount()
	{
		return mTaxiStationsCount;
	}
	
	public boolean areBusesEnabled()
	{
		return mAreBusesEnabled;
	}
	
	public int getBusesCount()
	{
		return mBusesCount;
	}
	
	public int getBusRoutesCount()
	{
		return mBusRoutesCount;
	}
	
	public int getTransportAllocationIndex()
	{
		return mTransportAllocationIndex;
	}
	
	public int getTimeConstraintIndex()
	{
		return mTimeConstraintIndex;
	}
}
