package dataStores;

import gui.screens.configurationScreen.SimulationConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dataStores.agentData.IAgentData;
import dataStores.agentData.IAgentDataStore;
import dataStores.userData.IUserData;
import dataStores.userData.IUserDataStore;
import dataStores.userData.UserData;
import util.movement.Movement;

public class SimulationDataStore implements IUserDataStore, IAgentDataStore
{
	private SimulationConfiguration mSimulationConfiguration;
	private Map<UUID, IUserData> mUserDataStores;
	private Map<UUID, IAgentData> mAgentsDataStores;
	public SimulationDataStore()
	{
		mUserDataStores = new HashMap<UUID, IUserData>();
		mAgentsDataStores = new HashMap<UUID, IAgentData>();
	}
	
	public void setSimulationConfiguration(SimulationConfiguration config)
	{
		mSimulationConfiguration = config;
	}
	public SimulationConfiguration getSimulationConfiguration()
	{
		return mSimulationConfiguration;
	}
	
	@Override
	public void addUserData(UUID userID, UserData userDataStore)
	{
		mUserDataStores.put(userID, userDataStore);
		addAgentData(userID, userDataStore);
	}
	public Map<UUID, IUserData> getUserDataStores()
	{
		return mUserDataStores;
	}
	
	@Override
	public void addAgentData(UUID agentID, IAgentData agentDataStore)
	{
		mAgentsDataStores.put(agentID, agentDataStore);
	}
	public Map<UUID, IAgentData> getAgentsDataStores()
	{
		return mAgentsDataStores;
	}
	
	public void addAgentMovements(UUID agentID, List<Movement> movements)
	{
		mAgentsDataStores.get(agentID).setMovements(movements);
	}
}
