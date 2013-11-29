package gui;

import com.google.inject.AbstractModule;

public class GUIModule extends AbstractModule
{
	@Override
	protected void configure() 
	{
		bind(SimulationGUI.class).toInstance(GUI.getInstance());
	}
}
