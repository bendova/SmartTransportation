package gui;

import dataStores.SimulationDataStore;

public interface ISmartTransportionGUI 
{
	void setSimulationData(SimulationDataStore agentData);
	void updateSimulationProgress(double progress);
}
