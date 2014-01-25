package conversations.userMediator.messages;

import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public interface ITransportServiceRequest 
{
	public Location getStartLocation();
	public Location getDestination();
	public String getMessage();
	public UUID getUserID();
	public UUID getUserAuthKey();
	public boolean isValid();	
}
