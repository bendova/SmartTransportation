package gui;

import gui.screens.configurationScreen.SimulationConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class GUIModule extends AbstractModule
{
	@Override
	protected void configure() 
	{
		bind(ISmartTransportionGUI.class).toInstance(GUI.getInstance());
	}
	
	@Provides
	public SimulationConfiguration getSimulationConfiguration() 
	{
		return GUI.getInstance().getSimulationConfiguration();
	}
}
