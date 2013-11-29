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
	
	public static int[][] mapConfiguration;
	
	public static final int DISTANCE_BETWEEN_REVISIONS = 10;
	
	private StatefulKnowledgeSession session;
	
	private NetworkAddress mMediatorNetworkAddress;
	
	private int mNextUserIndex = 0;
	private List<Taxi> mTaxies;
	
	private static List<AgentData> mAgentsData = new LinkedList<AgentData>();
	
	private SimulationGUI mGUI;
	
	@Inject
	private CityMap mCityMap;
	
	public Simulation(Set<AbstractModule> modules)
	{
		super(modules);
		mTaxies = new LinkedList<Taxi>();
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
		AddMediator(s);
		AddUsers(s);
		AddTaxiStations(s);
		s.addTimeDriven(this);
		
		session.setGlobal("logger", logger);
		session.setGlobal("DISTANCE_BETWEEN_REVISIONS", DISTANCE_BETWEEN_REVISIONS);
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
			User newUser = new User(Random.randomUUID(), "TaxiUser"+(mNextUserIndex++), 
					getRandomLocation(), getRandomLocation(), mMediatorNetworkAddress);
			s.addParticipant(newUser);
			session.insert(newUser);
		}
	}
	
	private void AddTaxiStations(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		for(int i = 0; i < taxiStationsCount; i++)
		{
			String stationName = "TaxiStation"+i;
			TaxiStation taxiStation = new TaxiStation(Random.randomUUID(), 
					stationName, getRandomLocation(), mMediatorNetworkAddress);
			s.addParticipant(taxiStation);
			AddTaxies(s, taxiStation.getNetworkAddress(), stationName);
		}
	}
	
	private void AddTaxies(Scenario s, NetworkAddress taxiStationNetworkAddress, 
			String taxiStationName)
	{
		for(int i = 0; i < taxiesCount; i++)
		{
			String taxiName = taxiStationName + "_TaxiCab"+i;
			Taxi newTaxi = new Taxi(Random.randomUUID(), taxiName, getRandomLocation(),
					taxiStationNetworkAddress);
			s.addParticipant(newTaxi);
			session.insert(newTaxi);
			mTaxies.add(newTaxi);
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
		
		mAgentsData.add(new AgentData(AgentType.TAXI_USER, agentName, locations));
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
