package SmartTransportation;

import java.util.Set;

import com.google.inject.AbstractModule;

public class SimulationMock extends Simulation
{	
	public SimulationMock(Set<AbstractModule> modules) 
	{
		super(modules);
	}
	
	@Override
	protected Set<AbstractModule> getModules() 
	{
		return SimulationTest.getModules();
	}
}
