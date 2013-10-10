import java.util.HashSet;
import java.util.Set;

import com.google.inject.AbstractModule;

import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Parameter;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.MoveHandler;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.location.area.Area;
import uk.ac.imperial.presage2.util.network.NetworkModule;
import uk.ac.imperial.presage2.core.util.random.Random;


public class Simulation extends InjectedSimulation
{
	@Parameter(name="areaSize")
	public int areaSize;
	
	@Parameter(name="usersCount")
	public int usersCount;
	
	@Parameter(name="taxiStationsCount")
	public int taxiStationsCount;
	
	public Simulation(Set<AbstractModule> modules)
	{
		super(modules);
	}
	
	@Override
	protected Set<AbstractModule> getModules() {
		Set<AbstractModule> modules = new HashSet<AbstractModule>();
		
		modules.add(Area.Bind.area2D(areaSize, areaSize));
		modules.add(new AbstractEnvironmentModule()
					// TODO .addActionHandler(RequestHandler.class)
					.addActionHandler(MoveHandler.class)
					.addParticipantEnvironmentService(ParticipantLocationService.class));
		modules.add(NetworkModule.fullyConnectedNetworkModule());
		
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) 
	{
		for(int i = 0; i < usersCount; i++)
		{
			int initialX = Random.randomInt(areaSize);
			int initialY = Random.randomInt(areaSize);
			Location startLocation = new Location(initialX, initialY);
			s.addParticipant(new User(Random.randomUUID(), "User"+i, startLocation));
		}
		
		Location[] startLocations = {new Location(0, 0),
		                             new Location(0, 10),
		                             new Location(10, 0),
		                             new Location(10, 10)};
		for(int i = 0; i < taxiStationsCount; i++)
		{
			s.addParticipant(new TaxiStationAgent(Random.randomUUID(), "TaxiStation"+i, startLocations[i]));
		}
	}

}
