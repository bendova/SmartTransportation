package testSmartTransportation;

import gui.ISmartTransportionGUI;
import gui.screens.configurationScreen.SimulationConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import dataStores.SimulationDataStore;

public class FakeGUIModule extends AbstractModule
{
	private FakeGUI mFakeGUI = new FakeGUI();
	private SimulationConfiguration mSimulationConfiguration;
	
	public FakeGUIModule(SimulationConfiguration config)
	{
		mSimulationConfiguration = config;
	}
	
	@Override
	protected void configure() 
	{
		bind(ISmartTransportionGUI.class).toInstance(mFakeGUI);
	}
	
	@Provides
	public SimulationConfiguration getSimulationConfiguration() 
	{
		return mSimulationConfiguration;
	}
}

class FakeGUI implements ISmartTransportionGUI
{
	@Override
	public void setSimulationData(SimulationDataStore agentData) 
	{
	}

	@Override
	public void updateSimulationProgress(double progress) 
	{
	}
}