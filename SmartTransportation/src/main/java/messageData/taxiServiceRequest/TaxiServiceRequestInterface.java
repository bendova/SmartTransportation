package messageData.taxiServiceRequest;

import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

public interface TaxiServiceRequestInterface 
{
	public Location getLocation();
	public String getMessage();
	public UUID getUserID();	
	public boolean isValid();	
}
