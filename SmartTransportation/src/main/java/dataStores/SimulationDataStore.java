package dataStores;

import gui.screens.configurationScreen.SimulationConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import util.movement.Movement;

public class SimulationDataStore 
{
	private SimulationConfiguration mSimulationConfiguration;
	private Map<UUID, UserDataStore> mUserDataStores;
	private Map<UUID, AgentDataStore> mAgentsDataStores;
	public SimulationDataStore()
	{
		mUserDataStores = new HashMap<UUID, UserDataStore>();
		mAgentsDataStores = new HashMap<UUID, AgentDataStore>();
	}
	
	public void setSimulationConfiguration(SimulationConfiguration config)
	{
		mSimulationConfiguration = config;
	}
	public SimulationConfiguration getSimulationConfiguration()
	{
		return mSimulationConfiguration;
	}
	public void addUserDataStore(UUID userID, UserDataStore userDataStore)
	{
		mUserDataStores.put(userID, userDataStore);
		addAgentDataStore(userID, userDataStore);
	}
	public Map<UUID, UserDataStore> getUserDataStores()
	{
		return mUserDataStores;
	}
	
	public void addAgentDataStore(UUID agentID, AgentDataStore agentDataStore)
	{
		mAgentsDataStores.put(agentID, agentDataStore);
	}
	public Map<UUID, AgentDataStore> getAgentsDataStores()
	{
		return mAgentsDataStores;
	}
	
	public void addAgentMovements(UUID agentID, List<Movement> movements)
	{
		mAgentsDataStores.get(agentID).setMovements(movements);
	}
}
