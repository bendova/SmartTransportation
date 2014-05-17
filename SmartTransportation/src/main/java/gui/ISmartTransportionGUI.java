package gui;

import gui.screens.configurationScreen.SimulationConfiguration;

import dataStores.SimulationDataStore;

public interface ISmartTransportionGUI 
{
	void setSimulationData(SimulationDataStore agentData);
	void updateSimulationProgress(double progress);
}
