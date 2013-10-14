package util;
import uk.ac.imperial.presage2.core.Time;

public class TimeStamp implements Time{
	private double time;
	public TimeStamp()
	{
		time = System.currentTimeMillis();
	}
	
	private TimeStamp(double time)
	{
		this.time = time;
	}
	
	@Override
	public String toString() {
		return Double.toString(time);
	}
	@Override
	public boolean equals(Time t) {
		if((t != null) && (t instanceof TimeStamp))
		{
			return (time == ((TimeStamp)t).time);
		}
		return false;
	}

	@Override
	public void increment() {
		time++;
	}

	@Override
	public void setTime(Time t) 
	{
		if(t != null)
		{
			time = t.intValue();
		}
	}

	@Override
	public boolean greaterThan(Time t) 
	{
		if(t != null)
		{
			return (time > t.intValue());
		}
		return false;
	}

	@Override
	public int intValue() 
	{
		return (int)time;
	}
	
	public double doubleValue() 
	{
		return time;
	}
	
	@Override
	public Time clone()
	{
		return new TimeStamp(this.time);
	}
}
