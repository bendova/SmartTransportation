package gui;

public class SimulationConfiguration 
{
	private int mDuration;
	private int mAreaSize;
	private int mTaxiesCount;
	private int mUsersCount;
	private int mTaxiStationsCount;
	
	public SimulationConfiguration(int duration, int areaSize, int usersCount, int taxiesCount,
			int taxiStationsCount)
	{
		assert(duration > 0);
		assert(areaSize > 0);
		assert(taxiesCount > 0);
		assert(usersCount > 0);
		assert(taxiStationsCount > 0);
		
		mDuration = duration;
		mAreaSize = areaSize;
		mUsersCount = usersCount;
		mTaxiesCount = taxiesCount;
		mTaxiStationsCount = taxiStationsCount;
	}
	
	public int getDuration()
	{
		return mDuration;
	}
	
	public int getAreaSize()
	{
		return mAreaSize;
	}
	
	public int getUsersCount()
	{
		return mUsersCount;
	}
	
	public int getTaxiesCount()
	{
		return mTaxiesCount;
	}
	
	public int getTaxiStationsCount()
	{
		return mTaxiStationsCount;
	}
}
