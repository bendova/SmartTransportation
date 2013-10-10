import java.util.Set;
import java.util.UUID;

import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;

public class User extends AbstractParticipant
{
	private Location mLocation;
	private ParticipantLocationService mLocationService;
	
	public User(UUID id, String name, Location location) {
		super(id, name);
		
		mLocation = location;
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() {
		Set<ParticipantSharedState> shareState =  super.getSharedState();
		shareState.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return shareState;
	}
	
	@Override
	public void initialise()
	{
		super.initialise();
		
		try 
		{
			mLocationService = getEnvironmentService(ParticipantLocationService.class);
		} 
		catch (UnavailableServiceException e) 
		{
			logger.warn(e);
		}
	}
	
	@Override
	protected void processInput(Input in) 
	{
		System.out.println(getName() + "::processInput()");
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		logger.info("::incrementTime() " + getTime());
	}
	
}
