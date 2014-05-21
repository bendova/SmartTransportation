package SmartTransportation;

import gui.GUI;
import gui.screens.configurationScreen.SimulationConfiguration;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import map.CityMap;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;

import uk.ac.imperial.presage2.core.simulator.RunnableSimulation;
import uk.ac.imperial.presage2.rules.RuleModule;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.network.NetworkModule;
import util.movement.TransportMoveHandler;

public class SimulationTest
{
	public static final String FAKE_RULES_PATH = Simulation.RULES_PATH + "FakeRules.drl";
	private static Set<AbstractModule> mModules;
	
	private int[][] mMapConfiguration;
	private Random mRandom;
	
	@Before
	public void init()
	{
		mRandom = new Random();
		initMapConfiguration();
		addDefaultModules();
	}
	
	private void initMapConfiguration()
	{
		mMapConfiguration = GUI.getMapConfiguration();
		Simulation.setMapConfiguration(mMapConfiguration);
	}
	
	public static Set<AbstractModule> getModules()
	{
		return mModules;
	}
	
	@Test
	public void withNoAgents()
	{
		setSimulationWithNoAgents();
		addFakeRules();
		startSimulation(10);
	}
	
	@Test
	public void withUsersOnly()
	{
		setSimulationWithUsersOnly();
		addUserRules();
		startSimulation(200);
	}
	
	@Test
	public void withUsersAndTaxies()
	{
		setSimulationWithUsersAndTaxies();
		addUserRules();
		startSimulation(200);
	}
	
	@Test
	public void withUsersAndBuses()
	{
		setSimulationWithUsersAndBuses();
		addUserRules();
		startSimulation(1200);
	}
	
	@Test
	public void withAllAgents()
	{
		setSimulationWithAllAgents();
		addUserRules();
		startSimulation(500);
	}
	
	private void addDefaultModules()
	{
		mModules = new HashSet<AbstractModule>();
		mModules.add(CityMap.Bind.cityMap2D(mMapConfiguration));
		mModules.add(new AbstractEnvironmentModule()
					.addActionHandler(TransportMoveHandler.class)
					.addParticipantEnvironmentService(ParticipantLocationService.class));
		mModules.add(NetworkModule.fullyConnectedNetworkModule());
	}
	
	private void setSimulationWithNoAgents()
	{
		SimulationConfiguration config = new SimulationConfiguration();
		mModules.add(new FakeGUIModule(config));
	}
	
	private void setSimulationWithAllAgents()
	{
		setSimulationConfig(new SimulationConfiguration()
			.setUsersCount(113)
			.setIsWalkingEnabled(mRandom.nextBoolean())
			.setAreTaxiesEnabled(mRandom.nextBoolean())
			.setTaxiesCount(17)
			.setTaxiStationsCount(5)
			.setAreBusesEnabled(mRandom.nextBoolean())
			.setBusesCount(11)
			.setBusRoutesCount(2)
			.setTimeContraintTypeIndex(mRandom.nextInt(Simulation.TimeConstraint.values().length))
			.setTransportAllocationMethodIndex(mRandom.nextInt(Simulation.TransportPreferenceAllocation.values().length))
			);
	}
	
	private void setSimulationWithUsersOnly()
	{
		setSimulationConfig(new SimulationConfiguration()
			.setUsersCount(13)
			.setIsWalkingEnabled(true));
	}
	
	private void setSimulationWithUsersAndTaxies()
	{
		setSimulationConfig(new SimulationConfiguration()
				.setUsersCount(5)
				.setAreTaxiesEnabled(true)
				.setTaxiesCount(13)
				.setTaxiStationsCount(2));
	}
	
	private void setSimulationWithUsersAndBuses()
	{
		setSimulationConfig(new SimulationConfiguration()
				.setUsersCount(7)
				.setIsWalkingEnabled(false)
				.setAreBusesEnabled(true)
				.setBusesCount(5)
				.setBusRoutesCount(2));
	}
	
	private void setSimulationConfig(SimulationConfiguration config)
	{
		mModules.add(new FakeGUIModule(config));
	}
	
	private void addFakeRules()
	{
		mModules.add(new RuleModule().addClasspathDrlFile(FAKE_RULES_PATH));
	}
	
	private void addUserRules()
	{
		mModules.add(new RuleModule().addClasspathDrlFile(Simulation.USER_RULES_PATH));
	}
	
	private void startSimulation(int finishTime)
	{
		final String[] args = getSimulationArguments(finishTime);
		try 
		{
			RunnableSimulation.main(args);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			Assert.fail();
		}
	}
	
	private String[] getSimulationArguments(int time)
	{
		String className = SimulationMock.class.getName();
		String finishTime = "finishTime=" + time;
		final String[] args = {className, finishTime};
		return args;
	}
}
