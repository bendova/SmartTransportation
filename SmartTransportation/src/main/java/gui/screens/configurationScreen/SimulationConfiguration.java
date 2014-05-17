package gui.screens.configurationScreen;

public class SimulationConfiguration 
{
	private int mTimeStepDuration 	= 10;
	private int mTimeStepsCount 	= 100;
	private int mAreaSize 			= 10;
	private int mPixelsPerAreaPoint = 10;
	private int mUsersCount 		= 0;
	private int mTaxiStationsCount 	= 0;
	private int mTaxiesCount 		= 0;
	private int mBusRoutesCount 	= 0;
	private int mBusesCount 		= 0;
	private int mTransportAllocationIndex 	= 0;
	private int mTimeConstraintIndex 		= 0;
	private boolean mIsWalkingEnabled = false;
	private boolean mAreTaxiesEnabled = false;
	private boolean mAreBusesEnabled = false;
	
	public SimulationConfiguration setTimeStepDuration(int duration)
	{
		assert(duration >= 0);
		
		mTimeStepDuration = duration;
		return this;
	}
	
	public SimulationConfiguration setTimeStepsCount(int timeStepsCount)
	{
		assert(timeStepsCount >= 0);
		
		mTimeStepsCount = timeStepsCount;
		return this;
	}

	public SimulationConfiguration setAreaSize(int areaSize)
	{
		assert(areaSize >= 0);
		
		mAreaSize = areaSize;
		return this;
	}

	public SimulationConfiguration setPixelsPerAreaPoint(int pixelsPerAreaPoint)
	{
		assert(pixelsPerAreaPoint >= 0);
		
		mPixelsPerAreaPoint = pixelsPerAreaPoint;
		return this;
	}
	
	public SimulationConfiguration setUsersCount(int usersCount)
	{
		assert(usersCount >= 0);
		
		mUsersCount = usersCount;
		return this;
	}
	
	public SimulationConfiguration setAreTaxiesEnabled(boolean areTaxiesEnabled)
	{
		mAreTaxiesEnabled = areTaxiesEnabled;
		return this;
	}
	
	public SimulationConfiguration setTaxiesCount(int taxiesCount)
	{
		assert(taxiesCount >= 0);
		
		mTaxiesCount = taxiesCount;
		return this;
	}
	
	public SimulationConfiguration setTaxiStationsCount(int taxiStationsCount)
	{
		assert(taxiStationsCount >= 0);
		
		mTaxiStationsCount = taxiStationsCount;
		return this;
	}
	
	public SimulationConfiguration setAreBusesEnabled(boolean areBusesEnabled)
	{
		mAreBusesEnabled = areBusesEnabled;
		return this;
	}
	
	public SimulationConfiguration setBusesCount(int busesCount)
	{
		assert(busesCount >= 0);
		
		mBusesCount = busesCount;
		return this;
	}
	
	public SimulationConfiguration setBusRoutesCount(int busRoutesCount)
	{
		assert(busRoutesCount >= 0);
		
		mBusRoutesCount = busRoutesCount;
		return this;
	}
	
	public SimulationConfiguration setIsWalkingEnabled(boolean isWalkingEnabled)
	{
		mIsWalkingEnabled = isWalkingEnabled;
		return this;
	}
	
	public SimulationConfiguration setTransportAllocationMethodIndex(int transportAllocationIndex)
	{
		assert(transportAllocationIndex >= 0);
		
		mTransportAllocationIndex = transportAllocationIndex;
		return this;
	}
	
	public SimulationConfiguration setTimeContraintTypeIndex(int timeContraintTypeIndex)
	{
		assert(timeContraintTypeIndex >= 0);
		
		mTimeConstraintIndex = timeContraintTypeIndex;
		return this;
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
