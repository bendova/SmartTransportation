package SmartTransportation;

import gui.GUIModule;
import gui.SimulationGUI;
import gui.configurationDialog.SimulationConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import map.CityMap;

import org.drools.runtime.StatefulKnowledgeSession;

import agents.Bus;
import agents.BusStation;
import agents.Mediator;
import agents.Taxi;
import agents.TaxiStation;
import agents.User;
import agents.User.TransportMethodSpeed;
import agents.User.TransportPreference;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import dataStores.SimulationDataStore;

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
import uk.ac.imperial.presage2.util.network.NetworkModule;
import uk.ac.imperial.presage2.core.util.random.Random;


public class Simulation extends InjectedSimulation implements TimeDriven
{
	@Parameter(name="areaSize")
	public int areaSize;
	
	
	public enum TransportPreferenceAllocation
	{
		RANDOM				("Random"),
		ALL_PREFER_WALKING	("All prefer walking"),
		ALL_PREFER_BUS		("All prefer bus"),
		ALL_PREFER_TAXI		("All prefer taxi");
		
		private String mDescription;
		private TransportPreferenceAllocation(String description)
		{
			mDescription = description;
		}
		public String getDescription()
		{
			return mDescription;
		}
	}
	
	public enum TimeConstraint
	{
		CONSTRAINT_1(5, "x5"),
		CONSTRAINT_2(7, "x7"),
		CONSTRAINT_3(11, "x11"),
		CONSTRAINT_4(23, "x23");
		
		private int mValue;
		private String mDescription;
		private TimeConstraint(int value, String description)
		{
			mValue = value;
			mDescription = description;
		}
		public int getValue()
		{
			return mValue;
		}
		public String getDescription()
		{
			return mDescription;
		}
	}
	
	private int mUsersCount;
	private boolean mIsWalkingEnabled;
	private boolean mAreTaxiesEnabled;
	private int mTaxiStationsCount;
	private int mTaxiesCount;
	private boolean mAreBusesEnabled;
	private int mBusesCount;
	private int mBusRoutesCount;
	private TransportPreferenceAllocation mTransportPrefAllocation;
	private TimeConstraint mTravelTimeConstraint;
	
	private static int[][] mMapConfiguration;
	
	public static final int DISTANCE_BETWEEN_REVISIONS = 100;
	
	private StatefulKnowledgeSession session;
	
	private NetworkAddress mMediatorNetworkAddress;
	
	private int mNextUserIndex = 0;
	private List<Taxi> mTaxies;
	private double mMaxSpeed;
	
	private SimulationGUI mGUI;
	private SimulationDataStore mSimulationDataStore;
	
	@Inject
	private CityMap mCityMap;
	
	private List<Location> mBusRoute1;
	private List<Location> mBusRoute2;
	
	private static int mSimDuration = 100;
	
	public Simulation(Set<AbstractModule> modules)
	{
		super(modules);
		
		mTaxies = new LinkedList<Taxi>();
		
		mBusRoute1 = new ArrayList<Location>();
		mBusRoute1.add(new Location(3, 5));
		mBusRoute1.add(new Location(20, 5));
		mBusRoute1.add(new Location(20, 16));
		mBusRoute1.add(new Location(3, 16));		
		mBusRoute2 = new ArrayList<Location>();
		mBusRoute2.add(new Location(3, 16));		
		mBusRoute2.add(new Location(20, 16));
		mBusRoute2.add(new Location(20, 5));
		mBusRoute2.add(new Location(3, 5));
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
		mMapConfiguration = mapConfig;
	}
	
	@Override
	protected Set<AbstractModule> getModules() {
		Set<AbstractModule> modules = new HashSet<AbstractModule>();
		
		modules.add(CityMap.Bind.cityMap2D(areaSize, areaSize, mMapConfiguration));
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
		initParameters();
		
		addMediator(s);
		addUsers(s);
		addTaxiStations(s);
		addBusStation(s);
		s.addTimeDriven(this);
		
		mSimDuration = s.getFinishTime().intValue();
		
		session.setGlobal("logger", logger);
		session.setGlobal("DISTANCE_BETWEEN_REVISIONS", DISTANCE_BETWEEN_REVISIONS);
	}
	
	private void initParameters()
	{
		SimulationConfiguration config = mGUI.getSimulationConfiguration();
		mUsersCount 		= config.getUsersCount();
		mIsWalkingEnabled 	= config.isWalkingEnabled();
		mAreTaxiesEnabled 	= config.areTaxiesEnabled();
		mTaxiStationsCount 	= config.getTaxiStationsCount();
		mTaxiesCount 		= config.getTaxiesCount();
		mAreBusesEnabled 	= config.areBusesEnabled();
		mBusesCount 		= config.getBusesCount();
		mBusRoutesCount 	= config.getBusRoutesCount();
		mTransportPrefAllocation 	= TransportPreferenceAllocation.values()[config.getTransportAllocationIndex()];
		mTravelTimeConstraint 		= TimeConstraint.values()[config.getTimeConstraintIndex()];
		
		mMaxSpeed = getMaximumSpeed();
		
		mSimulationDataStore = new SimulationDataStore();
		mSimulationDataStore.setSimulationConfiguration(config);
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
		
		for(int i = 0; i < mUsersCount; ++i)
		{
			Location startLocation = getRandomLocation();
			Location destination = getRandomLocation();
			double timeConstraint = getTimeConstraintForPath(startLocation, destination);
			User newUser = new User(Random.randomUUID(), "User"+(mNextUserIndex++), mCityMap,
					startLocation, destination, timeConstraint, mMediatorNetworkAddress, 
					getTransportPreference());
			newUser.enableWalking(mIsWalkingEnabled);
			newUser.enableBusUse(mAreBusesEnabled);
			newUser.enableTaxiUse(mAreTaxiesEnabled);
			newUser.setDataStore(mSimulationDataStore);
			s.addParticipant(newUser);
			session.insert(newUser);
		}
	}
	
	private void addTaxiStations(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		if(mAreTaxiesEnabled)
		{
			for(int i = 0; i < mTaxiStationsCount; ++i)
			{
				String stationName = "TaxiStation"+i;
				TaxiStation taxiStation = new TaxiStation(Random.randomUUID(), 
						stationName, getRandomLocation(), mMediatorNetworkAddress);
				s.addParticipant(taxiStation);
				addTaxies(s, taxiStation.getNetworkAddress(), stationName);
			}
		}
	}
	
	private void addTaxies(Scenario s, NetworkAddress taxiStationNetworkAddress, 
			String taxiStationName)
	{
		if(mAreTaxiesEnabled)
		{
			for(int i = 0; i < mTaxiesCount; ++i)
			{
				String taxiName = taxiStationName + "_TaxiCab"+i;
				Taxi newTaxi = new Taxi(Random.randomUUID(), taxiName, mCityMap,
						getRandomLocation(), taxiStationNetworkAddress);
				newTaxi.setDataStore(mSimulationDataStore);
				s.addParticipant(newTaxi);
				session.insert(newTaxi);
				mTaxies.add(newTaxi);
			}
		}
	}
	
	private void addBusStation(Scenario s)
	{
		assert(mMediatorNetworkAddress != null);
		
		if(mAreBusesEnabled)
		{
			String busStationName = "BusStation";
			BusStation busStation = new BusStation(Random.randomUUID(), busStationName, 
					mCityMap, getRandomLocation(), mMediatorNetworkAddress);
			busStation.addBusRoute(mBusRoute1);
			busStation.addBusRoute(mBusRoute2);			
			s.addParticipant(busStation);
			addBuses(s, busStation.getNetworkAddress(), busStationName);
		}
	}
	
	private void addBuses(Scenario s, NetworkAddress busStationAddress, String busStationName)
	{
		if(mAreBusesEnabled)
		{
			for(int i = 0; i < mBusesCount; ++i)
			{
				String name = busStationName + "_Bus" + i;
				Bus bus = new Bus(Random.randomUUID(), name, mCityMap, getRandomLocation(), 
						busStationAddress);
				bus.setDataStore(mSimulationDataStore);
				s.addParticipant(bus);
			}
		}
	}
	
	@Override
	public void incrementTime() 
	{
		logger.info("incrementTime() " + getSimluationPercentComplete());
		
		double progress = (double)(getCurrentSimulationTime().intValue() + 1)
							/ getSimulationFinishTime().intValue();
		mGUI.updateSimulationProgress(progress);
		
//		spawnUsersRandom(mScenario);
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
		mGUI.setSimulationData(mSimulationDataStore);
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
		
		return new Location(initialX, initialY);
	}

	private double getTimeConstraintForPath(Location start, Location end)
	{
		List<Location> path = mCityMap.getPath(start, end);
		double constraint = Math.ceil((double)path.size() / mMaxSpeed);
		return constraint * mTravelTimeConstraint.getValue();
	}
	
	private int getMaximumSpeed()
	{
		int taxiSpeed = Integer.MIN_VALUE;
		if(mAreTaxiesEnabled)
		{
			taxiSpeed = TransportMethodSpeed.TAXI_SPEED.getSpeed();
		}
		int busSpeed = Integer.MIN_VALUE;
		if(mAreBusesEnabled)
		{
			busSpeed = TransportMethodSpeed.BUS_SPEED.getSpeed();
		}
		int walkingSpeed = Integer.MIN_VALUE;
		if(mIsWalkingEnabled)
		{
			walkingSpeed = TransportMethodSpeed.WALKING_SPEED.getSpeed();
		}
		if((taxiSpeed >= busSpeed) && (taxiSpeed >= walkingSpeed))
		{
			return taxiSpeed;
		}
		else if ((busSpeed > taxiSpeed) && (busSpeed >= walkingSpeed))
		{
			return busSpeed;
		}
		else
		{
			return walkingSpeed;
		}
	}
	
	private TransportPreference getTransportPreference()
	{
		switch (mTransportPrefAllocation) 
		{
		case RANDOM:
			TransportPreference[] values = TransportPreference.values();
			return values[Random.randomInt(values.length)];
		case ALL_PREFER_BUS:
			return TransportPreference.BUS_PREFERENCE;
		case ALL_PREFER_TAXI:
			return TransportPreference.TAXI_PREFERENCE;
		case ALL_PREFER_WALKING:
			return TransportPreference.WALKING_PREFERENCE;
		default:
			assert(false) : "getTransportPreference() Allocation method not handled " + mTransportPrefAllocation;
			return null;
		}
	}
	
	/*	
	 * FIXME We get some probably-concurrency-related NullPointerExceptions
	 * when adding users in this manner
	*/
//	private void spawnUsersRandom(Scenario s)
//	{
//		int usersToAdd = Random.randomInt(usersCount);
//		for(int i = 0; i < usersToAdd; i++)
//		{
//			User user = new User(Random.randomUUID(), "User"+(mNextUserIndex++), 
//					getRandomLocation(), getRandomLocation(), mMediatorNetworkAddress);
//			s.addParticipant(user);
//			Time currentTime = getSimulator().getCurrentSimulationTime().clone();
//			user.initialiseTime(currentTime);
//			user.initialise();
//		}
//	}
	
	/*	
	 * FIXME We get some probably-concurrency-related NullPointerExceptions
	 * when adding users in this manner
	*/
//	private void spawnUsersAtInterval(Scenario s)
//	{
//		if(getSimulator().getCurrentSimulationTime().intValue() % 10 == 0)
//		{
//			User user = new User(Random.randomUUID(), "User"+(mNextUserIndex++), 
//					getRandomLocation(), getRandomLocation(), mMediatorNetworkAddress);
//			s.addParticipant(user);
//			Time currentTime = getSimulator().getCurrentSimulationTime().clone();
//			user.initialiseTime(currentTime);
//			user.initialise();
//		}
//	}
}
