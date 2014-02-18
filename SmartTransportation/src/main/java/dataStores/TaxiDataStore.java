package dataStores;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class TaxiDataStore 
{
	private String mName;
	private UUID mID;
	private List<Location> mPath;
	
	public TaxiDataStore(String taxiName, UUID taxiID)
	{
		assert(taxiName != null);
		assert(taxiID != null);
		
		mName = taxiName;
		mID = taxiID;
		mPath = new ArrayList<Location>();
	}
	public String getName()
	{
		return mName;
	}
	public UUID getID()
	{
		return mID;
	}
	public void setPathTraveled(List<Location> path)
	{
		mPath = path;
	}
	public List<Location> getPathTraveled()
	{
		return mPath;
	}
}
