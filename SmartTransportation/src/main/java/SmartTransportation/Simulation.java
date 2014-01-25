package SmartTransportation;

import gui.AgentData;
import gui.AgentData.AgentType;
import gui.GUI;
import gui.GUIModule;
import gui.SimulationGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javafx.util.Duration;

import map.CityMap;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

import agents.Bus;
import agents.BusStation;
import agents.Mediator;
import agents.Taxi;
import agents.TaxiStation;
import agents.User;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.sun.javafx.collections.MappingChange.Map;

import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Parameter;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.rules.RuleModule;
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
	
	@Parameter(name="busesCount")
	public int busesCount;
	
	public static int[][] mapConfiguration;
	
	public static final int DISTANCE_BETWEEN_REVISIONS = 100;
	
	private StatefulKnowledgeSession session;
	
	private NetworkAddress mMediatorNetworkAddress;
	
	private int mNextUserIndex = 0;
	private List<Taxi> mTaxies;
	
	private static List<AgentData> mAgentsData = new LinkedList<AgentData>();
	
	private SimulationGUI mGUI;
	
	@Inject
	private CityMap mCityMap;
	
	private List<Location> mBusStops;
	
	public Simulation(Set<AbstractModule> modules)
	{
		super(modules);
		mTaxies = new LinkedList<Taxi>();
		
		mBusStops = new ArrayList<Location>();
		mBusStops.add(new Location(3, 5));
		mBusStops.add(new Location(20, 5));
		mBusStops.add(new Location(20, 16));
		mBusStops.add(new Location(3, 16));
	}
	
	@Inject
	public void setSession(StatefulKnowledgeSession session) {
		this.session = session;
	}
	
	@Inject
	public void setGUI(SimulationGUI gui)
	{
		mGUI = gui;
	}
	
	public static void setMapConfiguration(int[][] mapConfig)
	{
		mapConfiguration = mapConfig;
	}
	
	@Override
	protected Set<AbstractModule> getModules() {
		Set<AbstractModule> modules = new HashSet<AbstractModule>();
		
		modules.add(CityMap.Bind.cityMap2D(areaSize, areaSize, mapConfiguration));
		modules.add(new AbstractEnvironmentModule()
					// TODO .addActionHandler(RequestHandler.class)
					.addActionHandler(MoveHandler.class)
					.addParticipantEnvironmentService(ParticipantLocationService.class));
		modules.add(NetworkModule.fullyConnectedNetworkModule());
		modules.add(new GUIModule());
		modules.add(new RuleModule().addClasspathDrlFile("MainRules.drl"));
		
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) 
	{
		addMediator(s);
		addUsers(s);
		addTaxiStations(s);
		addBusService(s);
		s.addTimeDriven(this);
		
		session.setGlobal("logger", logger);
		session.setGlobal("DISTANCE_BETWEEN_REVISIONS", DISTANCE_BETWEEN_REVISIONS);
	}
	
	private void addMediator(Scenario s)
	{
		Mediator mediator = new Mediator(Random.randomUUID(), "Mediator");
		s.addParticipant(mediator);
		mMediatorNetworkAddress = mediator.getNetworkAddress();
	}
	
	private void addUsers(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		for(int i = 0; i < usersCount; ++i)
		{
			User newUser = new User(Random.randomUUID(), "TaxiUser"+(mNextUserIndex++), 
					getRandomLocation(), getRandomLocation(), mMediatorNetworkAddress);
			s.addParticipant(newUser);
			session.insert(newUser);
		}
	}
	
	private void addTaxiStations(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		for(int i = 0; i < taxiStationsCount; ++i)
		{
			String stationName = "TaxiStation"+i;
			TaxiStation taxiStation = new TaxiStation(Random.randomUUID(), 
					stationName, getRandomLocation(), mMediatorNetworkAddress);
			s.addParticipant(taxiStation);
			addTaxies(s, taxiStation.getNetworkAddress(), stationName);
		}
	}
	
	private void addTaxies(Scenario s, NetworkAddress taxiStationNetworkAddress, 
			String taxiStationName)
	{
		for(int i = 0; i < taxiesCount; ++i)
		{
			String taxiName = taxiStationName + "_TaxiCab"+i;
			Taxi newTaxi = new Taxi(Random.randomUUID(), taxiName, getRandomLocation(),
					taxiStationNetworkAddress);
			s.addParticipant(newTaxi);
			session.insert(newTaxi);
			mTaxies.add(newTaxi);
		}
	}
	
	private void addBusService(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		String busStationName = "BusService";
		BusStation busStation = new BusStation(Random.randomUUID(), busStationName, 
				getRandomLocation(), mMediatorNetworkAddress);
		busStation.setBusRoute(mBusStops);
		s.addParticipant(busStation);
		addBuses(s, busStation.getNetworkAddress(), busStationName);
	}
	
	private void addBuses(Scenario s, NetworkAddress busStationAddress, String busStationName)
	{
		for(int i = 0; i < busesCount; ++i)
		{
			String name = busStationName + "_Bus" + i;
			Bus bus = new Bus(Random.randomUUID(), name, getRandomLocation(), 
					busStationAddress);
			s.addParticipant(bus);
		}
	}
	
	private Location getRandomLocation()
	{
		int initialX = Random.randomInt(areaSize);
		int initialY = Random.randomInt(areaSize);
		while(mCityMap.isValidLocation(initialX, initialY) == false)
		{
			initialX = Random.randomInt(areaSize);
			initialY = Random.randomInt(areaSize);
		}
		
		logger.info("initialX " + initialX);
		logger.info("initialY " + initialY);
		logger.info("mapConfiguration[initialX][initialY] " + mapConfiguration[initialX][initialY]);
		
		return new Location(initialX, initialY);
	}

	@Override
	public void incrementTime() 
	{
		logger.info("incrementTime() " + getSimulator().getCurrentSimulationTime());
		updateTaxiesInSession();
		session.fireAllRules();
	}
	
	private void updateTaxiesInSession()
	{
		for(Taxi taxi: mTaxies)
		{
			session.update(session.getFactHandle(taxi), taxi);
		}
	}
	
	@Override
	public void run() 
	{
		super.run();
		onSimulationComplete();
	}
	
	private void onSimulationComplete()
	{
		System.out.println("onSimulationComplete()");
		
		assert (mGUI != null) : "mGUI is null!"; 
		mGUI.setAreaSize(areaSize, areaSize);
		mGUI.setAgentsData(mAgentsData);
	}
	
	public static void addTaxiLocations(String agentName, ArrayList<Location> locations)
	{
		//System.out.println("addLocations() " + locations);
		assert(locations != null);
		
		mAgentsData.add(new AgentData(AgentType.TAXI_CAB, agentName, locations));
	}
	
	public static void addUserLocations(String agentName, ArrayList<Location> locations)
	{
		//System.out.println("addUserLocations() " + locations);
		assert(locations != null);
		
		mAgentsData.add(new AgentData(AgentType.USER, agentName, locations));
	}
	
	public static void addBusLocations(String agentName, ArrayList<Location> locations)
	{
		//System.out.println("addUserLocations() " + locations);
		assert(locations != null);
		
		mAgentsData.add(new AgentData(AgentType.BUS, agentName, locations));
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
