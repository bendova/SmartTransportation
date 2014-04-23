package gui;

import com.google.inject.AbstractModule;

public class GUIModule extends AbstractModule
{
	@Override
	protected void configure() 
	{
		bind(ISmartTransportionGUI.class).toInstance(GUI.getInstance());
	}
}
