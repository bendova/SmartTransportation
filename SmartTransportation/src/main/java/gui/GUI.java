package gui;

import gui.agents.AgentDataForMap;
import gui.agents.AgentDataForMap.AgentType;
import gui.charts.ChartsMenuController;
import gui.components.LabelsPaneController;
import gui.screens.LoadingScreen;
import gui.screens.configurationScreen.ConfigureSimulationController;
import gui.screens.configurationScreen.SimulationConfiguration;
import gui.timeline.TimeLineController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import dataStores.SimulationDataStore;
import dataStores.agentData.AgentData;
import dataStores.agentData.IAgentData;
import SmartTransportation.Simulation;
import SmartTransportation.Simulation.TimeConstraint;
import SmartTransportation.Simulation.TransportPreferenceAllocation;
import uk.ac.imperial.presage2.core.simulator.RunnableSimulation;
import javafx.application.Application;
import javafx.application.Platform;
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
	public static final String SCREENS_PATH = LAYOUTS_PATH + "screens/";
	public static final String COMPONENTS_PATH = LAYOUTS_PATH + "components/";
	
	private static final String CONFIGURATION_DIALOG_LAYOUT	= SCREENS_PATH + "ConfigurationDialog.fxml";
	private static final String LOADING_DIALOG_LAYOUT 		= SCREENS_PATH + "LoadingDialog.fxml";
	
	private static final String MENU_BAR_LAYOUT 	= COMPONENTS_PATH + "MenuBar.fxml";
	private static final String CHARTS_MENU_LAYOUT 	= COMPONENTS_PATH + "ChartsMenu.fxml";
	private static final String LABELS_PANE_LAYOUT 	= COMPONENTS_PATH + "LabelsPane.fxml";
	
	private static final String SAVE_FILE_PATH = "save/SavedSimulationConfig.sav";
	
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
	private Pane mLegendPane;
	private Stage mStage;
	private Group mSimulationGroup;
	private ToggleButton mPlayPauseToggle;
	private Slider mTimeLineSlider;
	
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
		controller.setConfiguration(loadSavedConfiguration());
		controller.setOnStartCallback( new Callback<SimulationConfiguration, Void>() 
		{
			@Override
			public Void call(SimulationConfiguration param) 
			{
				saveConfiguration(param);
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
	
	private SimulationConfiguration loadSavedConfiguration()
	{
		SimulationConfiguration configuration = null;
		try 
		{
			File file = new File(getConfigurationSaveFilePath());
			if(file.exists())
			{
				FileInputStream fileInputStream = new FileInputStream(file);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				
				configuration = (SimulationConfiguration)objectInputStream.readObject();
				objectInputStream.close();
			}
		}
		catch (Exception e) 
		{
			// do nothing
		}

		if(configuration == null)
		{
			configuration = new SimulationConfiguration();
		}
		return configuration;
	}
	
	private void saveConfiguration(SimulationConfiguration config)
	{
		if(config.shouldSaveConfiguration())
		{
			try 
			{
				FileOutputStream fileOutputStream = new FileOutputStream(getSaveFile());
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(config);
				objectOutputStream.close();
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private File getSaveFile()
	{
		File saveFile = new File(getConfigurationSaveFilePath());
		if(saveFile.exists() == false)
		{
			if(saveFile.getParentFile().exists() == false)
			{
				saveFile.getParentFile().mkdirs();
			}
			
			try 
			{
				saveFile.createNewFile();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return saveFile;
	}
	
	private String getConfigurationSaveFilePath()
	{
		String saveFilePath = "";
		try 
		{
			URI codePath = GUI.class.getProtectionDomain().
					getCodeSource().getLocation().toURI();
			saveFilePath = codePath.resolve(SAVE_FILE_PATH).getPath();
		} 
		catch (URISyntaxException e) 
		{
			e.printStackTrace();
		}
		return saveFilePath;
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
			simulation.setOnSucceeded(event -> beginAnimation());
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
		
		loadComponents();
		loadSimulation();
		
		initRootGroup();
		
		loadSimulationStage();
	}
	
	private void loadComponents()
	{
		initTimeLineTimer();
		mTimeLinePane = loadTimeLine();
		mChartsMenuPane = loadChartsMenu();
		mLegendPane = loadLegendPane();
		mMap = loadMap();
	}
	
	private void initTimeLineTimer()
	{
		// TODO replace this with a Timeline
		mTimeLineTimer = new Timer();
		mTimeLineTimer.schedule(new TimerTask() 
		{
			@Override
			public void run() 
			{
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						updateTimeLine();
					}
				});
			}
		}, 0, (int)mTimeStepDuration.toMillis());
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
		EventHandler<MouseEvent> timeLineEventHandler = (event ->
				jumpToKeyFrame(mTimeLineSlider.getValue()));
		mTimeLineSlider.setOnMouseClicked(timeLineEventHandler);
		mTimeLineSlider.setOnMouseDragged(timeLineEventHandler);
		mPlayPauseToggle = controller.getPlayPauseButton();
		mPlayPauseToggle.setOnAction(event -> togglePlay());
		return controller.getMenuBar();
	}
	
	private Pane loadChartsMenu()
	{
		System.out.println("GUI::loadChartsMenu()");
		
		ChartsMenuController chartsMenuController = (ChartsMenuController)
				(loadNode(CHARTS_MENU_LAYOUT)).getController();
		chartsMenuController.setData(mSimulationDataStore);		
		
		Pane container = chartsMenuController.getContainer();
		container.translateXProperty().set(0);
		container.translateYProperty().bind(
				mStage.heightProperty().subtract(container.heightProperty()).divide(2));
		return container;
	}
	
	private Pane loadLegendPane()
	{
		System.out.println("GUI::loadLegendPane()");
		
		LabelsPaneController legendPaneController = (LabelsPaneController)
				(loadNode(LABELS_PANE_LAYOUT)).getController();
		
		Pane container = legendPaneController.getContainer();
		container.translateXProperty().bind(
				mStage.widthProperty()
						.subtract(container.widthProperty()));
		container.translateYProperty().bind(
				mStage.heightProperty()
						.subtract(container.heightProperty())
						.divide(2));
		
		return container;
	}
	
	private void loadSimulation()
	{
		mSimulationGroup = new Group();
		
		mSimulationGroup.getChildren().add(mMap);
		mSimulationGroup.translateYProperty().bind(mTimeLinePane.heightProperty().add(10));
		mSimulationGroup.translateXProperty().bind(mStage.widthProperty().subtract(mMapWidth).divide(2));
		addAgentsToGroup(mSimulationGroup.getChildren());
	}
	
	private void initRootGroup()
	{
		mRoot = new Group();
		ObservableList<Node> rootChildren = mRoot.getChildren();
		rootChildren.add(mTimeLinePane);
		rootChildren.add(mSimulationGroup);
		rootChildren.add(mChartsMenuPane);
		rootChildren.add(mLegendPane);
	}
	
	private void loadSimulationStage()
	{
		Scene scene = new Scene(mRoot, mMapWidth, mMapHeight, false);
		scene.setFill(Color.WHITE);
		mStage.setScene(scene);
		mStage.setTitle("Smart Transportation");
		mStage.setMaximized(true);
		mStage.show();
	}
	
	private void addAgentsToGroup(ObservableList<Node> childrenList)
	{
		List<AgentDataForMap> userAgentDataList = new ArrayList<AgentDataForMap>(); 
		for(AgentDataForMap agentData: mAgentsDataForMap)
		{
			if(agentData.getAgentType() != AgentType.USER)
			{
				agentData.setRootChildrenList(childrenList);
			}
			else 
			{
				userAgentDataList.add(agentData);
			}
		}
		for(AgentDataForMap agentData : userAgentDataList)
		{
			agentData.setRootChildrenList(childrenList);
		}
	}
	
	public static FXMLLoader loadNode(String path)
	{
		final FXMLLoader loader;		
		try 
		{
			loader = new FXMLLoader();
			loader.setBuilderFactory(new JavaFXBuilderFactory());
			loader.setLocation(GUI.class.getResource(path));
			loader.load();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
		return loader;
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
		
		for(AgentDataForMap agentData: mAgentsDataForMap)
		{
			agentData.playAnimation();
		}
		mAnimationState = AnimationState.PLAYING;
	}
	
	private void pause()
	{
		System.out.println("GUI::pause()");
		
		for(AgentDataForMap agentData: mAgentsDataForMap)
		{
			agentData.pauseAnimation();
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
			agentData.restartAnimation();
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
		if(mAnimationState == AnimationState.PLAYING)
		{
			mPlayPauseToggle.setSelected(false);
		}
		mAnimationState = AnimationState.PAUSED;
		
		for (AgentDataForMap agentData : mAgentsDataForMap) 
		{
			agentData.jumpToInAnimation(Duration.millis(frame * mTimeStepDuration.toMillis()));
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
		
		if(agentsData != null)
		{
			mSimulationDataStore = agentsData;
			initMapData();
		}
	}
	
	private void initMapData()
	{
		mAgentsDataForMap = new LinkedList<AgentDataForMap>();
		fillAgentsDataForMap();
	}
	
	private void fillAgentsDataForMap()
	{
		Map<UUID, IAgentData> agentDataStores = mSimulationDataStore.getAgentsDataStores();
		Iterator<Map.Entry<UUID, IAgentData>> iterator = agentDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			IAgentData data = iterator.next().getValue();
			AgentDataForMap agentDataForMap = new AgentDataForMap(data.getAgentType(), data.getName(), 
					data.getMovements(), data.getStartLocation(), 
					mPixelsPerAreaPoint, mTimeStepDuration);
			mAgentsDataForMap.add(agentDataForMap);
			
			agentDataForMap.setOnAnimationFinished(event ->
				{
					--mAgentsAnimatingCount;
					
					if(mAgentsAnimatingCount == 0)
					{
						System.out.println("Animation finished!");
						
						mPlayPauseToggle.setSelected(false);
						mAnimationState = AnimationState.FINISHED;
					}
				}
			);
		}
		mAgentsAnimatingCount = mAgentsDataForMap.size();
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
}
