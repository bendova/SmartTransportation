package gui;

import gui.configurationDialog.SimulationConfiguration;

import java.util.List;

public interface SimulationGUI {
	void setAreaSize(int width, int height);
	void setAgentsData(List<AgentData> agentData);
	void updateSimulationProgress(double progress);
	SimulationConfiguration getSimulationConfiguration();
	int[][] getMapConfiguration();
}
