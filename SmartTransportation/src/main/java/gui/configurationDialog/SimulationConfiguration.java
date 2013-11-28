package gui.configurationDialog;

public class SimulationConfiguration 
{
	private int mTimeStepDuration;
	private int mTimeStepsCount;
	private int mAreaSize;
	private int mPixelsPerAreaPoint;
	private int mTaxiesCount;
	private int mUsersCount;
	private int mTaxiStationsCount;
	
	public SimulationConfiguration(int duration, int timeStepsCount, int areaSize, int pixelsPerAreaPoint, int usersCount, int taxiesCount,
			int taxiStationsCount)
	{
		assert(duration > 0);
		assert(timeStepsCount > 0);
		assert(areaSize > 0);
		assert(pixelsPerAreaPoint > 0);
		assert(taxiesCount > 0);
		assert(usersCount > 0);
		assert(taxiStationsCount > 0);
		
		mTimeStepDuration = duration;
		mTimeStepsCount = timeStepsCount;
		mAreaSize = areaSize;
		mPixelsPerAreaPoint = pixelsPerAreaPoint;
		mUsersCount = usersCount;
		mTaxiesCount = taxiesCount;
		mTaxiStationsCount = taxiStationsCount;
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
	
	public int getTaxiesCount()
	{
		return mTaxiesCount;
	}
	
	public int getTaxiStationsCount()
	{
		return mTaxiStationsCount;
	}
}
