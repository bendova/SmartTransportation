package gui;

import gui.AgentData.AgentType;
import gui.agents.AgentNodeController;
import gui.configurationDialog.ConfigureSimulationController;
import gui.configurationDialog.SimulationConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import SmartTransportation.Simulation;

import uk.ac.imperial.presage2.core.simulator.RunnableSimulation;
import uk.ac.imperial.presage2.util.location.Location;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.*;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.stage.Stage;
import javafx.util.*;

public class GUI extends Application implements SimulationGUI
{
	private final String LAYOUTS_PATH = "../layouts/";
	private final String CONFIGURATION_DIALOG_LAYOUT 	= LAYOUTS_PATH + "ConfigurationDialog.fxml";
	private final String LOADING_DIALOG_LAYOUT 		 	= LAYOUTS_PATH + "LoadingDialog.fxml";
	private final String MENU_BAR_LAYOUT 				= LAYOUTS_PATH + "MenuBar.fxml";
	
	private double mMapWidth = 300;
	private double mMapHeight = 300;

	private double mPixelsPerAreaPoint = 60;
	private int mTimeStepsCount = 0;
	private Duration mTimeStepDuration = new Duration(200);
	
	private List<AgentData> mAgentsData = new LinkedList<AgentData>();
	
	private Group mRoot;
	private Group mMap;
	private Pane mMenuBar;
	private Stage mStage;
	private Group mAgentsGroup;
	private ToggleButton mPlayPauseToggle;
	private Slider mTimeLineSlider;
	
	private Timer mTimeLineTimer;
	
	private int mAgentsAnimatingCount = 0;
	
	enum AnimationState
	{
		PLAYING,
		PAUSED,
		FINISHED
	}
	private AnimationState mAnimationState = AnimationState.PAUSED;
	
	private static GUI mInstance;
	public static GUI getInstance()
	{
		return mInstance;
	}
	
	private int mMapLayout[][] = {
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
		mStage.setTitle("Configure Simulation");
	}
	
	private void startSimulation(SimulationConfiguration config)
	{
		mTimeStepDuration = new Duration(config.getTimeStepDuration());
		mPixelsPerAreaPoint = config.getPixelsPerAreaPoint();
		mTimeStepsCount = config.getTimeStepsCount();
		
		Simulation.setMapConfiguration(mMapLayout);
		String className = "SmartTransportation.Simulation";
		String finishTime = "finishTime=" + mTimeStepsCount;
		String areaSize = "areaSize=" + config.getAreaSize();
		String usersCount = "usersCount=" + config.getUsersCount();
		String taxiesCount = "taxiesCount=" + config.getTaxiesCount();
		String taxiStationsCount = "taxiStationsCount=" + config.getTaxiStationsCount();
		String busesCount = "busesCount=" + config.getBusesCount();
		final String[] args = {className, finishTime, areaSize, usersCount, 
				taxiesCount, taxiStationsCount, busesCount};
		try {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void openProgressDialog(Stage parent)
	{
		loadScene(LOADING_DIALOG_LAYOUT);
		mStage.setTitle("Simulating...");
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
		mMenuBar = loadMenuBar();
		
		mAgentsGroup = new Group();
		mAgentsGroup.getChildren().add(mMap);
		mAgentsGroup.translateYProperty().bind(mMenuBar.heightProperty().add(10));
		mAgentsGroup.translateXProperty().bind(mStage.widthProperty().subtract(mMapWidth).divide(2));
		addAgentsToScene(mAgentsGroup.getChildren());
		
		mRoot = new Group();
		mRoot.getChildren().add(mMenuBar);
		mRoot.getChildren().add(mAgentsGroup);
		
		Scene scene = new Scene(mRoot, mMapWidth, mMapHeight, Color.WHITE);
		mStage.setScene(scene);
		mStage.setTitle("Smart Transportation");
		
//		mStage.setFullScreen(true);
		mStage.show();
	}
	
	private Group loadMap()
	{
		Random random = new Random();
		Group map = new Group();
		Rectangle building;
		int coordX = 0;
		int coordY = 0;
		for (int i = 0; i < mMapLayout.length; i++) 
		{
			for (int j = 0; j < mMapLayout[i].length; j++) 
			{
				if(mMapLayout[i][j] == 1)
				{
					random.nextDouble();
					building = RectangleBuilder.create().
							translateX(coordX).translateY(coordY).
							width(mPixelsPerAreaPoint).height(mPixelsPerAreaPoint).
//							fill(Color.GRAY).
							fill(new Color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 1)).
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
	
	private Pane loadMenuBar()
	{
		MenuBarController controller = (MenuBarController)
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
	
	private void addAgentsToScene(ObservableList<Node> childrenList)
	{
		List<AgentData> userDataList = new LinkedList<AgentData>();
		for(AgentData agentData: mAgentsData)
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
		for(AgentData agentData: userDataList)
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
	
	private void addAnimations(final Node agentNode, AgentData agentData)
	{
		final String agentName = agentData.getName();

		SequentialTransition sequentialTransition = new SequentialTransition();
		agentData.setAnimation(sequentialTransition);
		ArrayList<Location> locations = agentData.getLocations();
			
		Location startLocation = locations.remove(0);
		double startX = startLocation.getX();
		double startY = startLocation.getY();

//		System.out.println("Plotting path for " + agentName + 
//				", startX " + startX + ", startY " + startY);
		
		Transition startTransition = TranslateTransitionBuilder.create().
				node(agentNode).duration(Duration.ZERO).
				fromX(0).toX(startX).
				fromY(0).toY(startY).
				cycleCount(1).autoReverse(false).
				build();
		sequentialTransition.getChildren().add(startTransition);
		
		FadeTransition fadeInTransition = FadeTransitionBuilder.create().
				node(agentNode).duration(Duration.millis(100)).
				toValue(1).
				build();
		sequentialTransition.getChildren().add(fadeInTransition);
		
		for(Location location: locations)
		{
			double nextX = location.getX() * mPixelsPerAreaPoint;
			double nextY = location.getY() * mPixelsPerAreaPoint;
			
			Transition transition;
			if((nextX == startX) && (nextY == startY))
			{
				transition = new PauseTransition(mTimeStepDuration);
			}
			else
			{
//				System.out.println("Plotting path for " + agentName + 
//						", nextX " + nextX + ", nextY " + nextY);
				transition = TranslateTransitionBuilder.create().
						node(agentNode).duration(mTimeStepDuration).
						fromX(startX).toX(nextX).
						fromY(startY).toY(nextY).
						cycleCount(1).autoReverse(false).
						build();
				startX = nextX;
				startY = nextY;
			}
			sequentialTransition.getChildren().add(transition);
		}
		FadeTransition fadeOutTransition = FadeTransitionBuilder.create().
				node(agentNode).duration(Duration.millis(100)).
				toValue(0.2).
				build();
		
		sequentialTransition.getChildren().add(fadeOutTransition);
		
		sequentialTransition.setCycleCount(1);
		sequentialTransition.setAutoReverse(false);
		sequentialTransition.setOnFinished(new EventHandler<ActionEvent>() 
		{
			@Override
			public void handle(ActionEvent event) 
			{
				System.out.println("Animation completed for " + agentName);
				mAgentsAnimatingCount--;
				
//				System.out.println("Animation completed mAgentsAnimatingCount " + mAgentsAnimatingCount);
				
				if(mAgentsAnimatingCount == 0)
				{
					System.out.println("Animation finished!");
					
					mPlayPauseToggle.setSelected(false);
					mAnimationState = AnimationState.FINISHED;
				}
			}
		});
		mAgentsAnimatingCount++;
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
		for(AgentData agentData: mAgentsData)
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
		
		for(AgentData agentData: mAgentsData)
		{
			agentData.getAnimation().pause();
		}
		mAnimationState = AnimationState.PAUSED;
	}
	
	private void replay()
	{
		System.out.println("GUI::replay()");
		
		mTimeLineSlider.setValue(0);
		mAgentsAnimatingCount = mAgentsData.size();
		for(AgentData agentData: mAgentsData)
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
		
		for (AgentData agentData : mAgentsData) 
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
	
	public void setAgentsData(List<AgentData> agentsData)
	{
		assert(agentsData != null);
		
		mAgentsData = agentsData;
	}
	
	public int[][] getMapConfiguration()
	{
		return mMapLayout;
	}
}
