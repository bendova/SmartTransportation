import java.util.UUID;

import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;


public class TaxiStationAgent extends AbstractParticipant
{
	Location mLocation;
	public TaxiStationAgent(UUID id, String name, Location location) 
	{
		super(id, name);
		mLocation = location;
	}

	@Override
	protected void processInput(Input in) 
	{
		// TODO Auto-generated method stub
		
	}

}
