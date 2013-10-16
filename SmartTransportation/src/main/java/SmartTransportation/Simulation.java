package SmartTransportation;
import java.util.HashSet;
import java.util.Set;

import org.drools.command.ExecuteCommand;


import agents.Mediator;
import agents.Taxi;
import agents.TaxiStation;
import agents.User;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import uk.ac.imperial.presage2.core.Time;
import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.environment.EnvironmentConnector;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.NetworkConnectorFactory;
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


public class Simulation extends InjectedSimulation implements TimeDriven
{
	@Parameter(name="areaSize")
	public int areaSize;
	
	@Parameter(name="usersCount")
	public int usersCount;
	
	@Parameter(name="taxiStationsCount")
	public int taxiStationsCount;
	
	@Parameter(name="taxiesCount")
	public int taxiesCount;
	
	private NetworkAddress mMediatorNetworkAddress;
	
	private int mNextUserIndex = 0;
	private boolean mAreUsersAdded = false;
	
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
		AddMediator(s);
		AddUsers(s);
		AddTaxiStations(s);
		s.addTimeDriven(this);
	}
	
	private void AddMediator(Scenario s)
	{
		Mediator mediator = new Mediator(Random.randomUUID(), "Mediator");
		s.addParticipant(mediator);
		mMediatorNetworkAddress = mediator.getNetworkAddress();
	}
	
	private void AddUsers(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		for(int i = 0; i < usersCount; i++)
		{
			s.addParticipant(new User(Random.randomUUID(), "TaxiUser"+(mNextUserIndex++), 
					getRandomLocation(), getRandomLocation(), mMediatorNetworkAddress));
		}
	}
	
	private void AddTaxiStations(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		Location[] startLocations = {new Location(0, 0),
                new Location(0, 10),
                new Location(10, 0),
                new Location(10, 10)};
		for(int i = 0; i < taxiStationsCount; i++)
		{
			TaxiStation taxiStation = new TaxiStation(Random.randomUUID(), 
					"TaxiStation"+i, startLocations[i], mMediatorNetworkAddress);
			s.addParticipant(taxiStation);
			AddTaxies(s, taxiStation.getNetworkAddress());
		}
	}
	
	private void AddTaxies(Scenario s, NetworkAddress taxiStationNetworkAddress)
	{
		for(int i = 0; i < taxiesCount; i++)
		{
			s.addParticipant(new Taxi(Random.randomUUID(), "TaxiCab"+i, getRandomLocation(),
					taxiStationNetworkAddress));
		}
	}
	
	private Location getRandomLocation()
	{
		int initialX = Random.randomInt(areaSize);
		int initialY = Random.randomInt(areaSize);
		return new Location(initialX, initialY);
	}

	@Override
	public void incrementTime() 
	{
		logger.info("incrementTime() " + getSimulator().getCurrentSimulationTime());
		
//		FIXME
//		AddUsers(scenario);
	}
	
	/*	
	 * FIXME fix this to be able to add a random number of users
	 * each round
	*/
//	private void AddUsers(Scenario s)
//	{
//		int usersToAdd = Random.randomInt(usersCount);
//		for(int i = 0; i < usersToAdd; i++)
//		{
//			User user = new User(Random.randomUUID(), "TaxiUser"+(mNextUserIndex++), 
//					getRandomLocation(), getRandomLocation(), mMediatorNetworkAddress);
//			Injector injector = this.getInjector();
//			injector.injectMembers(user);
//			user.initialiseTime(getSimulator().getCurrentSimulationTime());
//			user.initialise();
//			s.addParticipant(user);
//		}
//	}
}
