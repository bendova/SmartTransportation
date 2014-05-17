package gui;

import gui.agents.AgentDataForMap;
import gui.agents.AgentNodeController;
import gui.charts.ChartsMenuController;
import gui.charts.Chart;
import gui.charts.transportMethodsUsed.TransportMethodsUsedWindow;
import gui.charts.transportResults.TransportResultsWindow;
import gui.charts.userDataTable.UserTableData;
import gui.charts.userDataTable.UserTableWindow;
import gui.screens.LoadingScreen;
import gui.screens.configurationScreen.ConfigureSimulationController;
import gui.screens.configurationScreen.SimulationConfiguration;
import gui.timeline.TimeLineController;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import agents.User.TransportMode;

import dataStores.SimulationDataStore;
import dataStores.AgentDataStore;
import dataStores.UserDataStore;

import SmartTransportation.Simulation;
import SmartTransportation.Simulation.TimeConstraint;
import SmartTransportation.Simulation.TransportPreferenceAllocation;

import uk.ac.imperial.presage2.core.simulator.RunnableSimulation;
import uk.ac.imperial.presage2.util.location.Location;
import util.movement.Movement;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.*;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.stage.Stage;
import javafx.util.*;

public class GUI extends Application implements ISmartTransportionGUI
{
	public static final String LAYOUTS_PATH = "/layouts/";
	private final String CONFIGURATION_DIALOG_LAYOUT 	= LAYOUTS_PATH + "ConfigurationDialog.fxml";
	private final String LOADING_DIALOG_LAYOUT 		 	= LAYOUTS_PATH + "LoadingDialog.fxml";
	private final String MENU_BAR_LAYOUT 				= LAYOUTS_PATH + "MenuBar.fxml";
	private final String CHARTS_MENU_LAYOUT 			= LAYOUTS_PATH + "ChartsMenu.fxml";
	
	private double mMapWidth = 300;
	private double mMapHeight = 300;

	private double mPixelsPerAreaPoint = 60;
	private int mTimeStepsCount = 0;
	private Duration mTimeStepDuration = new Duration(200);
	
	private SimulationDataStore mSimulationDataStore;
	private List<AgentDataForMap> mAgentsDataForMap = new LinkedList<AgentDataForMap>();
	
	private Group mRoot;
	private Group mMap;
	private Pane mTimeLinePane;
	private Pane mChartsMenuPane;
	private Stage mStage;
	private Group mAgentsGroup;
	private ToggleButton mPlayPauseToggle;
	private Slider mTimeLineSlider;
	
	private TransportMethodsUsedWindow mTransportModesUseChart;
	private TransportResultsWindow mTransportResultsChart;
	private UserTableWindow mUserDataTableChart;
	
	private Timer mTimeLineTimer;
	private LoadingScreen mProgressDialogController;
	
	private int mAgentsAnimatingCount = 0;
	private SimulationConfiguration mSimulationConfiguration;
	enum AnimationState
	{
		PLAYING,
		PAUSED,
		FINISHED
	}
	private AnimationState mAnimationState = AnimationState.PAUSED;
	
	private static GUI mInstance;
	private static int mMapLayout[][] = {
			{1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1}, 
			{1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1}, 
			{1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1},
			{1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},
			{1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1},
			{0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
			{1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0},
			{0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0},
			{1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1},
			{1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1},
			{1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1}, 
			{1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1}, 
			{1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1},
			{1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},
			{1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1},
			{0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
			{1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0},
			{0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0},
			{1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1},
			{1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1}
			};
	
	public static GUI getInstance()
	{
		return mInstance;
	}
	
	public SimulationConfiguration getSimulationConfiguration()
	{
		return mSimulationConfiguration;
	}
	
	public static int[][] getMapConfiguration()
	{
		return mMapLayout;
	}
	
	public static void main(String[] args)
	{
		Application.launch(args);
	}
	
	@Override
	public void start(Stage stage)
	{
		System.out.println("GUI::start()");
		
		mInstance = this;
		mStage = stage;
		openConfigurationDialog();
	}
	
	@Override
	public void stop()
	{
		if(mTimeLineTimer != null)
		{
			mTimeLineTimer.cancel();
		}
	}
	
	private void openConfigurationDialog()
	{
		ConfigureSimulationController controller = 
				(ConfigureSimulationController)loadScene(CONFIGURATION_DIALOG_LAYOUT);
		controller.setOnStartCallback( new Callback<SimulationConfiguration, Void>() 
		{
			@Override
			public Void call(SimulationConfiguration param) 
			{
				startSimulation(param);
				return null;
			}
		});
		
		TransportPreferenceAllocation[] allocationTypes = TransportPreferenceAllocation.values();
		List<String> allocationNames = new ArrayList<String>();
		for (int i = 0; i < allocationTypes.length; ++i) 
		{
			allocationNames.add(allocationTypes[i].getDescription());
		}
		controller.setTransportAllocationTypes(allocationNames);
		
		TimeConstraint[] constraintTypes = TimeConstraint.values();
		List<String> constraintNames = new ArrayList<String>();
		for (int i = 0; i < constraintTypes.length; ++i) 
		{
			constraintNames.add(constraintTypes[i].getDescription());
		}
		controller.setsTimeConstraints(constraintNames);
		
		mStage.setTitle("Configure Simulation");
	}
	
	private void startSimulation(SimulationConfiguration config)
	{
		mSimulationConfiguration = config;
		mTimeStepDuration = new Duration(config.getTimeStepDuration());
		mPixelsPerAreaPoint = config.getPixelsPerAreaPoint();
		mTimeStepsCount = config.getTimeStepsCount();
		setAreaSize(config.getAreaSize(), config.getAreaSize());
		
		Simulation.setMapConfiguration(mMapLayout);
		String className = Simulation.class.getName();
		String finishTime = "finishTime=" + mTimeStepsCount;
		final String[] args = {className, finishTime};
		try 
		{
			openProgressDialog(mStage);
			Task<Void> simulation = new Task<Void>()
			{
				@Override
				protected Void call() throws Exception 
				{
					RunnableSimulation.main(args);
					return null;
				}
			};
			simulation.setOnSucceeded(new EventHandler<WorkerStateEvent>() 
			{
				@Override
				public void handle(WorkerStateEvent event) 
				{
					beginAnimation();
				}
			});
			Thread simulationThread = new Thread(simulation);
			simulationThread.start();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private void openProgressDialog(Stage parent)
	{
		mProgressDialogController = (LoadingScreen)loadScene(LOADING_DIALOG_LAYOUT);
		mStage.setTitle("Simulating...");
	}
	
	@Override
	public void updateSimulationProgress(double progress)
	{
		System.out.println("GUI::updateSimulationProgress() " + progress);
		
		mProgressDialogController.updateProgress(progress);
	}
	
	public void beginAnimation()
	{
		System.out.println("GUI::beginAnimation()");
		
		// TODO replace this with a Timeline
		mTimeLineTimer = new Timer();
		mTimeLineTimer.schedule(new TimerTask() 
		{
			@Override
			public void run() 
			{
				updateTimeLine();
			}
		}, 0, (int)mTimeStepDuration.toMillis());
		
		mMap = loadMap();
		mTimeLinePane = loadTimeLine();
		mChartsMenuPane = loadChartsMenu();
		mAgentsGroup = new Group();
		mAgentsGroup.getChildren().add(mMap);
		mAgentsGroup.translateYProperty().bind(mTimeLinePane.heightProperty().add(10));
		mAgentsGroup.translateXProperty().bind(mStage.widthProperty().subtract(mMapWidth).divide(2));
		addAgentsToGroup(mAgentsGroup.getChildren());
		
		mRoot = new Group();
		mRoot.getChildren().add(mTimeLinePane);
		mRoot.getChildren().add(mAgentsGroup);
		mRoot.getChildren().add(mChartsMenuPane);
		Scene scene = new Scene(mRoot, mMapWidth, mMapHeight, Color.WHITE);
		mStage.setScene(scene);
		mStage.setTitle("Smart Transportation");
		
		mStage.centerOnScreen();
		mStage.show();
	}
	
	private Group loadMap()
	{
		Group map = new Group();
		Rectangle building;
		int coordX = 0;
		int coordY = 0;
		for (int i = 0; i < mMapLayout.length; ++i) 
		{
			for (int j = 0; j < mMapLayout[i].length; ++j) 
			{
				if(mMapLayout[i][j] == 1)
				{
					building = RectangleBuilder.create().
							translateX(coordX).translateY(coordY).
							width(mPixelsPerAreaPoint).height(mPixelsPerAreaPoint).
							fill(Color.SILVER).
							build();
					map.getChildren().add(building);
				}
				coordY += mPixelsPerAreaPoint;
			}
			coordY = 0;
			coordX += mPixelsPerAreaPoint;
		}
		
		return map;
	}
	
	private Pane loadTimeLine()
	{
		TimeLineController controller = (TimeLineController)
				(loadNode(MENU_BAR_LAYOUT)).getController();
		controller.getBackground().widthProperty().bind(mStage.widthProperty());
		mTimeLineSlider = controller.getTimeLineSlider();
		mTimeLineSlider.setBlockIncrement(1);
		mTimeLineSlider.setMax(mTimeStepsCount);
		EventHandler<MouseEvent> timeLineEventHandler = new EventHandler<MouseEvent>() 
		{
			@Override
			public void handle(MouseEvent event) 
			{
				jumpToKeyFrame(mTimeLineSlider.getValue());
			}
		};
		mTimeLineSlider.setOnMouseClicked(timeLineEventHandler);
		mTimeLineSlider.setOnMouseDragged(timeLineEventHandler);
		mPlayPauseToggle = controller.getPlayPauseButton();
		mPlayPauseToggle.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				togglePlay();
			}
		});
		return controller.getMenuBar();
	}
	
	private Pane loadChartsMenu()
	{
		System.out.println("GUI::loadChartsMenu()");
		
		ChartsMenuController chartsMenuController = (ChartsMenuController)
				(loadNode(CHARTS_MENU_LAYOUT)).getController();
				
		chartsMenuController.setOnShowTransportMethodUsed(new Callback<Void, Void>() 
		{
			@Override
			public Void call(Void param) 
			{
				toggleWindow(mTransportModesUseChart);
				return null;
			}
		});
		chartsMenuController.setOnShowUserTransportResults(new Callback<Void, Void>() 
		{
			@Override
			public Void call(Void param) 
			{
				toggleWindow(mTransportResultsChart);
				return null;
			}
		});
		chartsMenuController.setOnShowUserDataTable(new Callback<Void, Void>() 
		{
			@Override
			public Void call(Void param) 
			{
				toggleWindow(mUserDataTableChart);
				return null;
			}
		});
		Pane container = chartsMenuController.container;
		container.translateXProperty().set(0);
		container.translateYProperty().bind(
				mStage.heightProperty().subtract(container.heightProperty()).divide(2));
		return container;
	}
	
	private Parent loadScene(String scenePath)
	{
		Parent rootGroup = null;
		FXMLLoader loader;		
		try 
		{
			loader = new FXMLLoader();
			loader.setBuilderFactory(new JavaFXBuilderFactory());
			loader.setLocation(getClass().getResource(scenePath));
			InputStream inputStream = getClass().getResourceAsStream(scenePath);
			rootGroup = (Parent)loader.load(inputStream);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
		
		Scene scene = new Scene(rootGroup);
		mStage.setScene(scene);
		mStage.show();
		return (Parent) loader.getController();
	}
	
	private void addAgentsToGroup(ObservableList<Node> childrenList)
	{
		List<AgentDataForMap> userDataList = new LinkedList<AgentDataForMap>();
		for(AgentDataForMap agentData: mAgentsDataForMap)
		{
			switch (agentData.getType()) 
			{
			case TAXI_CAB:
			case BUS:
				Node agentNode = loadAgentNode(agentData.getLayoutPath(), agentData.getName());
				addAnimations(agentNode, agentData);
				agentData.setNode(agentNode);
				childrenList.add(agentNode);
				break;
			case USER:
				// the user shapes must be displayed ABOVE
				// the other shapes, so add them afterwards
				userDataList.add(agentData);
				break;
			default:
				break;
			}
		}
		for(AgentDataForMap agentData: userDataList)
		{
			Node agentNode = loadAgentNode(agentData.getLayoutPath(), agentData.getName());
			addAnimations(agentNode, agentData);
			agentData.setNode(agentNode);
			childrenList.add(agentNode);
		}
	}
	
	private Node loadAgentNode(String path, String name)
	{
		AgentNodeController controller = (AgentNodeController)
				(loadNode(path)).getController();
		controller.setTitle(name);
		return controller.getNode();
	}
	
	private FXMLLoader loadNode(String path)
	{
		final FXMLLoader loader;		
		try 
		{
			loader = new FXMLLoader();
			loader.setBuilderFactory(new JavaFXBuilderFactory());
			loader.setLocation(getClass().getResource(path));
			loader.load();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
		return loader;
	}
	
	private void addAnimations(final Node agentNode, AgentDataForMap agentData)
	{
		final String agentName = agentData.getName();

		SequentialTransition sequentialTransition = new SequentialTransition();
		agentData.setAnimation(sequentialTransition);
			
		Location startLocation = agentData.getStartLocation();
		double currentX = startLocation.getX() * mPixelsPerAreaPoint;
		double currentY = startLocation.getY() * mPixelsPerAreaPoint;

		Transition startTransition = TranslateTransitionBuilder.create().
				node(agentNode).duration(Duration.millis(100)).
				fromX(0).toX(currentX).
				fromY(0).toY(currentY).
				cycleCount(1).autoReverse(false).
				interpolator(Interpolator.LINEAR).
				build();
		sequentialTransition.getChildren().add(startTransition);
		
		FadeTransition fadeInTransition = FadeTransitionBuilder.create().
				node(agentNode).duration(Duration.millis(100)).
				toValue(1).
				build();
		sequentialTransition.getChildren().add(fadeInTransition);
		
		System.out.println("addAnimations() for " + agentName);
		
		List<Movement> movements = agentData.getMoveData();
		int currentTime = 0;
		for(Movement movement: movements)
		{
			int movementStartTime = movement.getStartTime();
			
			if(currentTime < movementStartTime)
			{
				Transition pauseTransition = new PauseTransition(mTimeStepDuration.
						multiply((movementStartTime - currentTime)));
				currentTime = movementStartTime;
				sequentialTransition.getChildren().add(pauseTransition);
			}
			
			Location location = movement.getLocation();
			double nextX = location.getX() * mPixelsPerAreaPoint;
			double nextY = location.getY() * mPixelsPerAreaPoint;
			
			int timeTakenPerUnitDistance = movement.getTimeTakenPerUnitDistance();
			currentTime += timeTakenPerUnitDistance;
			
			Transition moveTransition = TranslateTransitionBuilder.create().
					node(agentNode).duration(mTimeStepDuration.multiply(timeTakenPerUnitDistance)).
					fromX(currentX).toX(nextX).
					fromY(currentY).toY(nextY).
					interpolator(Interpolator.LINEAR).
					cycleCount(1).autoReverse(false).
					build();
			currentX = nextX;
			currentY = nextY;
			sequentialTransition.getChildren().add(moveTransition);
		}
		FadeTransition fadeOutTransition = FadeTransitionBuilder.create().
				node(agentNode).duration(Duration.millis(100)).
				toValue(0.2).
				build();
		
		sequentialTransition.getChildren().add(fadeOutTransition);
		
		sequentialTransition.setCycleCount(1);
		sequentialTransition.setAutoReverse(false);
		sequentialTransition.setInterpolator(Interpolator.LINEAR);
		sequentialTransition.setOnFinished(new EventHandler<ActionEvent>() 
		{
			@Override
			public void handle(ActionEvent event) 
			{
				System.out.println("Animation completed for " + agentName);
				--mAgentsAnimatingCount;
				
				if(mAgentsAnimatingCount == 0)
				{
					System.out.println("Animation finished!");
					
					mPlayPauseToggle.setSelected(false);
					mAnimationState = AnimationState.FINISHED;
				}
			}
		});
		++mAgentsAnimatingCount;
	}
	
	private void togglePlay()
	{
		switch(mAnimationState)
		{
		case FINISHED:
			replay();
			break;
		case PAUSED:
			play();
			break;
		case PLAYING:
			pause();
			break;
		default:
			// there is a case that we are not handling
			assert false : "AnimationState case " + mAnimationState + " not handled!"; 
			break;
		}
	}
	
	private void play()
	{
		System.out.println("GUI::play()");
		
		Animation animation;
		for(AgentDataForMap agentData: mAgentsDataForMap)
		{
			animation = agentData.getAnimation();
			if(animation.getTotalDuration().greaterThan(animation.getCurrentTime()))
			{
				animation.play();
			}
		}
		mAnimationState = AnimationState.PLAYING;
	}
	
	private void pause()
	{
		System.out.println("GUI::pause()");
		
		for(AgentDataForMap agentData: mAgentsDataForMap)
		{
			agentData.getAnimation().pause();
		}
		mAnimationState = AnimationState.PAUSED;
	}
	
	private void replay()
	{
		System.out.println("GUI::replay()");
		
		mTimeLineSlider.setValue(0);
		mAgentsAnimatingCount = mAgentsDataForMap.size();
		for(AgentDataForMap agentData: mAgentsDataForMap)
		{
			agentData.getAnimation().playFromStart();
		}
		mAnimationState = AnimationState.PLAYING;
	}
	
	private void updateTimeLine()
	{		
		switch(mAnimationState)
		{
		case PLAYING:
			mTimeLineSlider.increment();
			break;
		case PAUSED:
		case FINISHED:
			// do nothing
			break;
		}
	}
	
	private void jumpToKeyFrame(double frame)
	{
		System.out.println("GUI::jumpToKeyFrame frame " + frame);
		
		if(mAnimationState == AnimationState.PLAYING)
		{
			mPlayPauseToggle.setSelected(false);
		}
		mAnimationState = AnimationState.PAUSED;
		
		for (AgentDataForMap agentData : mAgentsDataForMap) 
		{
			agentData.getAnimation().pause();
			agentData.getAnimation().jumpTo(Duration.millis(frame * mTimeStepDuration.toMillis()));
			
			System.out.println("GUI::jumpToKeyFrame agentData.getName() " + agentData.getName());
		}
	}
	
	public void setAreaSize(int width, int height)
	{
		assert(width > 0);
		assert(height > 0);
		
		mMapWidth = width * mPixelsPerAreaPoint;
		mMapHeight = height * mPixelsPerAreaPoint;
		
		System.out.println("mMapWidth " + mMapWidth);
		System.out.println("mMapHeight " + mMapHeight);
	}
	
	public void setSimulationData(SimulationDataStore agentsData)
	{
		assert(agentsData != null);
		
		mSimulationDataStore = agentsData;
		initCharts();
		initMapData();
	}
	
	private void initCharts()
	{
		initTransportModesUseChart();
		initTravelTimesChart();
		initUserDataTable();
	}
	
	private void initTransportModesUseChart()
	{
		mTransportModesUseChart = new TransportMethodsUsedWindow();
		
		TransportMode[] transportModes = TransportMode.values();
		int[] transportModeUse = new int[transportModes.length];
		Map<UUID, UserDataStore> userDataStores = mSimulationDataStore.getUserDataStores();
		Iterator<Map.Entry<UUID, UserDataStore>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			UserDataStore data = iterator.next().getValue();
			++transportModeUse[data.getTransportMethodUsed().ordinal()];
		}
		Map<String, Double> pieData = new HashMap<String, Double>();
		for (int i = 0; i < transportModeUse.length; ++i) 
		{
			pieData.put(transportModes[i].getName(), (double)transportModeUse[i]);
		}
		mTransportModesUseChart.setData(pieData);
	}
	
	private void initTravelTimesChart()
	{
		mTransportResultsChart = new TransportResultsWindow();
		TreeMap<Number, Number> chartData = new TreeMap<Number, Number>();
		
		Map<UUID, UserDataStore> userDataStores = mSimulationDataStore.getUserDataStores();
		Iterator<Map.Entry<UUID, UserDataStore>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			UserDataStore data = iterator.next().getValue();
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
		
		mTransportResultsChart.setData(chartData);
	}
	
	private void initUserDataTable()
	{
		ObservableList<UserTableData> userTableDataList = FXCollections.observableArrayList();
		
		Map<UUID, UserDataStore> userDataStores = mSimulationDataStore.getUserDataStores();
		Iterator<Map.Entry<UUID, UserDataStore>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			UserDataStore data = iterator.next().getValue();
			
			userTableDataList.add(new UserTableData(data.getName(), data.getID(), 
					data.getHasReachedDestination(), data.getActualTravelTime(), 
					data.getTargetTravelTime(), data.getTransportPreference(), 
					data.getTransportMethodUsed()));
		}
		
		mUserDataTableChart = new UserTableWindow();
		mUserDataTableChart.setData(userTableDataList);
	}
	
	private void initMapData()
	{
		mAgentsDataForMap = new LinkedList<AgentDataForMap>();
		fillAgentsDataForMap();
	}
	
	private void fillAgentsDataForMap()
	{
		Map<UUID, AgentDataStore> agentDataStores = mSimulationDataStore.getAgentsDataStores();
		Iterator<Map.Entry<UUID, AgentDataStore>> iterator = agentDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			AgentDataStore data = iterator.next().getValue();
			mAgentsDataForMap.add(new AgentDataForMap(data.getAgentType(), data.getName(), 
					data.getMovements(), data.getStartLocation()));
		}
	}

	private void toggleWindow(Chart window)
	{
		System.out.println("GUI::toggleWindow() window " + window);
		System.out.println("GUI::toggleWindow() window.isIconified() " + window.isIconified());
		
		if(window.isIconified())
		{
			window.maximize();
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
