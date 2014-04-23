package gui;

import gui.screens.configurationScreen.SimulationConfiguration;

import dataStores.SimulationDataStore;

public interface ISmartTransportionGUI 
{
	void setAreaSize(int width, int height);
	void setSimulationData(SimulationDataStore agentData);
	void updateSimulationProgress(double progress);
	SimulationConfiguration getSimulationConfiguration();
	int[][] getMapConfiguration();
}
