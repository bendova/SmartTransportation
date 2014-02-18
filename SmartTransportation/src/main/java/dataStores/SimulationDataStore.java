package dataStores;

import gui.configurationDialog.SimulationConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SimulationDataStore 
{
	private SimulationConfiguration mSimulationConfiguration;
	private Map<UUID, UserDataStore> mUserDataStores;
	private Map<UUID, TaxiDataStore> mTaxiDataStores;
	private Map<UUID, BusDataStore> mBusDataStores;
	public SimulationDataStore()
	{
		mUserDataStores = new HashMap<UUID, UserDataStore>();
		mTaxiDataStores = new HashMap<UUID, TaxiDataStore>();
		mBusDataStores = new HashMap<UUID, BusDataStore>();
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
	}
	public Map<UUID, UserDataStore> getUserDataStores()
	{
		return mUserDataStores;
	}
	public void addTaxiDataStore(UUID taxiID, TaxiDataStore taxiDataStore)
	{
		mTaxiDataStores.put(taxiID, taxiDataStore);
	}
	public Map<UUID, TaxiDataStore> getTaxiDataStores()
	{
		return mTaxiDataStores;
	}
	public void addBusDataStore(UUID busID, BusDataStore busDataStore)
	{
		mBusDataStores.put(busID, busDataStore);
	}
	public Map<UUID, BusDataStore> getBusDataStores()
	{
		return mBusDataStores;
	}
}
