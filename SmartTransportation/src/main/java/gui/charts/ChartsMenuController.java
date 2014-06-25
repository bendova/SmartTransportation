package gui.charts;

import gui.charts.userDataTable.UserTableData;
import gui.charts.userDataTable.UserTableWindow;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import agents.User.TransportMode;
import dataStores.SimulationDataStore;
import dataStores.userData.IUserData;
import dataStores.userData.UserData;
import dataStores.userData.UserEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ChartsMenuController extends Parent
{
	@FXML
	private Pane container;
	@FXML
	private Button showTransportMethodsUsed; 
	@FXML
	private Button showUserTransportResults; 
	@FXML
	private Button showUserDataTable; 
	@FXML
	private Button showUserTimeline; 
	@FXML
	private Button showDestinationReachedPercentage; 
	
	private SimulationDataStore mSimulationDataStore;
	private PieChartWindow mTransportModesUseChart;
	private PieChartWindow mReachedDestinationChart;
	private TransportResultsWindow mTransportResultsChart;
	private UserTableWindow mUserDataTableChart;
	private UserTimelineWindow mUserTimelineWindow;
	
	public Pane getContainer()
	{
		return container;
	}
	
	public void setData(SimulationDataStore simulationDataStore)
	{
		assert(simulationDataStore != null);
		
		if(simulationDataStore != null)
		{
			mSimulationDataStore = simulationDataStore;
			initCharts();
			initializeEventHandlers();
		}
	}
	
	private void initCharts()
	{
		initTransportModesUseChart();
		initReachedDestinationChart();
		initTravelTimesChart();
		initUserDataTable();
		initUserTimelineWindow();
	}
	
	private void initTransportModesUseChart()
	{
		TransportMode[] transportModes = TransportMode.values();
		
		int[] transportModeUse = new int[transportModes.length];
		Map<UUID, IUserData> userDataStores = mSimulationDataStore.getUserDataStores();
		Iterator<Map.Entry<UUID, IUserData>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			IUserData data = iterator.next().getValue();
			++transportModeUse[data.getTransportMethodUsed().ordinal()];
		}
		
		int usersCount = userDataStores.size();
		Map<String, Integer> pieData = new HashMap<String, Integer>();
		for (int i = 0; i < transportModeUse.length; ++i) 
		{
			int percent = (int) (((double)transportModeUse[i] / usersCount) * 100);
			
			pieData.put(transportModes[i].getName() + "(" + percent + " %)",
					transportModeUse[i]);
		}
		
		mTransportModesUseChart = new PieChartWindow(pieData, "Transport modes used");
	}
	
	private void initReachedDestinationChart()
	{
		int reachedDestinationCount = 0;
		Map<UUID, IUserData> userDataStores = mSimulationDataStore.getUserDataStores();
		Iterator<Map.Entry<UUID, IUserData>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			IUserData data = iterator.next().getValue();
			if(data.getHasReachedDestination())
			{
				++reachedDestinationCount;
			}
		}
		
		Map<String, Integer> pieData = new HashMap<String, Integer>();
		int usersCount = userDataStores.size();
		int reachedDestinationPercent = (int) (((double)reachedDestinationCount / usersCount) * 100);
		String reachedDestinationDescription = "Reached destination (" + 
				reachedDestinationPercent + " %)";
		pieData.put(reachedDestinationDescription, reachedDestinationCount);
		
		int notReachDestinationCount = userDataStores.size() - reachedDestinationCount;
		int notReachedDestinationPercent = 100 - reachedDestinationPercent;
		String notReachedDestinationDescription = "Failed (" + 
				notReachedDestinationPercent + " %)";
		pieData.put(notReachedDestinationDescription, notReachDestinationCount);
		
		mReachedDestinationChart = new PieChartWindow(pieData, "Destination reach percentages");
	}
	
	private void initTravelTimesChart()
	{
		TreeMap<Number, Number> chartData = new TreeMap<Number, Number>();
		
		Map<UUID, IUserData> userDataStores = mSimulationDataStore.getUserDataStores();
		Iterator<Map.Entry<UUID, IUserData>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			IUserData data = iterator.next().getValue();
			int deltaTravelTime = (int)(data.getActualTravelTime() - data.getTargetTravelTime());
			Number usersCount = chartData.get(deltaTravelTime);
			if(usersCount != null)
			{
				chartData.put(deltaTravelTime, usersCount.intValue()+1);
			}
			else 
			{
				chartData.put(deltaTravelTime, 1);
			}
		}
		
		mTransportResultsChart = new TransportResultsWindow(chartData);
	}
	
	private void initUserDataTable()
	{
		ObservableList<UserTableData> userTableDataList = FXCollections.observableArrayList();
		
		Map<UUID, IUserData> userDataStores = mSimulationDataStore.getUserDataStores();
		Iterator<Map.Entry<UUID, IUserData>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			IUserData data = iterator.next().getValue();
			
			userTableDataList.add(new UserTableData(data.getName(), data.getID(), 
					data.getHasReachedDestination(), data.getActualTravelTime(), 
					data.getTargetTravelTime(), data.getTransportPreference(), 
					data.getTransportMethodUsed()));
		}
		
		mUserDataTableChart = new UserTableWindow(userTableDataList);
	}
	
	private void initUserTimelineWindow()
	{
		mUserTimelineWindow = new UserTimelineWindow(mSimulationDataStore.getUserDataStores());
	}
	
	private void initializeEventHandlers()
	{
		showTransportMethodsUsed.setOnAction(
			event -> toggleWindow(mTransportModesUseChart)
		);
		showUserTransportResults.setOnAction(
			event -> toggleWindow(mTransportResultsChart)
		);
		showUserDataTable.setOnAction(
			event -> toggleWindow(mUserDataTableChart)
		);
		showDestinationReachedPercentage.setOnAction(
			event -> toggleWindow(mReachedDestinationChart)
		);
		showUserTimeline.setOnAction(
			event -> toggleWindow(mUserTimelineWindow)
		);
	}
	
	private void toggleWindow(Stage window)
	{
		if(window.isIconified())
		{
			window.setIconified(false);
		}
		else if(window.isShowing())
		{
			window.close();
		}
		else 
		{
			window.show();
		}
	}
}
