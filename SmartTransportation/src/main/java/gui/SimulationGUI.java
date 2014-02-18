package gui;

import gui.configurationDialog.SimulationConfiguration;

import dataStores.SimulationDataStore;

public interface SimulationGUI 
{
	void setAreaSize(int width, int height);
	void setSimulationData(SimulationDataStore agentData);
	void updateSimulationProgress(double progress);
	SimulationConfiguration getSimulationConfiguration();
	int[][] getMapConfiguration();
}
