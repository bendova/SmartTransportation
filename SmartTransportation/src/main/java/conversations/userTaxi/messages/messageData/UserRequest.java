package conversations.userTaxi.messages.messageData;
import java.util.UUID;

import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.location.Location;

public class UserRequest 
{
	private Location mLocation;
	private String mMessage;
	private UUID mID;
	public UserRequest(Location location, String message)
	{
		assert(location != null);
		assert(message != null);
		
		mLocation = location;
		mMessage = message;
		mID = Random.randomUUID();
	}
	
	public Location GetLocation() {
		return mLocation;
	}
	
	public String GetMessage() {
		return mMessage;
	}
	
	public UUID getID()
	{
		return mID;
	}
	
	@Override
	public int hashCode()
	{
		return this.getID().hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if((obj != null) && (obj instanceof UserRequest))
		{
			return (this.getID() == ((UserRequest)obj).getID());
		}
		return false;
	}
}
