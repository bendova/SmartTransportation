package Messages;

import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public class TaxiOrder 
{
	private UUID mUserID;
	private Location mLocation;
	
	public TaxiOrder(UUID userID, Location location)
	{
		assert(userID != null);
		assert(location != null);
		
		mUserID = userID;
		mLocation = location;
	}
	
	public UUID getUserID() 
	{
		return mUserID;
	}
	
	public Location getUserLocation()
	{
		return mLocation;
	}
}
