package gui;

import java.util.*;

import SmartTransportation.Simulation;

import uk.ac.imperial.presage2.core.simulator.RunnableSimulation;
import uk.ac.imperial.presage2.util.location.Location;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.*;

public class GUI extends Application
{
	private final double PIXELS_PER_AREA_UNIT = 70;
	private final double TAXI_SIZE = 10;
	private final double USER_SIZE = 10;
	
	private List<AgentData> mTaxiAgentsData = new LinkedList<AgentData>();
	private List<AgentData> mUserAgentsData = new LinkedList<AgentData>();
	private double mMapWidth = 300;
	private double mMapHeight = 300;
	
	private int mTimeStepsCount = 1;
	private Duration mTimeStepDuration = new Duration(1000);
	
	private Group mRoot = new Group();
	private Stage mStage;
	private int mAgentsAnimatingCount = 0;
	
	public static void main(String[] args)
	{
		System.out.println(args);
		Application.launch(args);
	}
	
	public void start(Stage stage)
	{
		System.out.println("GUI::start()");
		
		mStage = stage;
		openConfigurationDialog(mStage, new SubmitConfigurationHandler() 
		{
			@Override
			public void handle(SimulationConfiguration config) 
			{
				startSimulation(config);
			}
		});
	}
	
	private void startSimulation(SimulationConfiguration config)
	{
		Simulation.setGUI(this);
		
		String className = "SmartTransportation.Simulation";
		String finishTime = "finishTime=" + config.getDuration();
		String areaSize = "areaSize=" + config.getAreaSize();
		String usersCount = "usersCount=" + config.getUsersCount();
		String taxiesCount = "taxiesCount=" + config.getTaxiesCount();
		String taxiStationsCount = "taxiStationsCount=" + config.getTaxiStationsCount();
		String[] args = {className, finishTime, areaSize, usersCount, taxiesCount, taxiStationsCount};
		try {
			RunnableSimulation.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void openConfigurationDialog(Stage parent, SubmitConfigurationHandler submitHandler)
	{
		ConfigureSimulationDialog dialog = new ConfigureSimulationDialog(parent, submitHandler);
		dialog.sizeToScene();
		dialog.show();
	}
	
	public void beginAnimation()
	{
		System.out.println("GUI::beginAnimation()");
		
		mStage.setTitle("Smart Transportation");
		
//		MenuBar menuBar = new MenuBar();
//		menuBar.prefWidthProperty().bind(mStage.widthProperty());
//		
//		Menu menu = new Menu("Controls");
//		MenuItem menuItem = new MenuItem("Play/Pause");
//		menuItem.setOnAction(new EventHandler<ActionEvent>() 
//		{
//			@Override
//			public void handle(ActionEvent arg0) 
//			{
//				// TODO
//				
//			}
//		});
//		menu.getItems().add(menuItem);
//		menuBar.getMenus().add(menu);
//		
//		mRoot.getChildren().add(menuBar);
		
		Scene scene = new Scene(mRoot, mMapWidth, mMapHeight, Color.WHITE);
		addAgentsToScene(mRoot.getChildren());
		mStage.setScene(scene);
		mStage.show();
	}
	
	public void setAreaSize(int width, int height)
	{
		assert(width > 0);
		assert(height > 0);
		
		mMapWidth = width * PIXELS_PER_AREA_UNIT;
		mMapHeight = height * PIXELS_PER_AREA_UNIT;
		
		System.out.println("mMapWidth " + mMapWidth);
		System.out.println("mMapHeight " + mMapHeight);
	}
	
	public void setTimeStepDuration(Duration timeStepDuration)
	{
		assert(timeStepDuration != null);
		assert(timeStepDuration.toMillis() != Double.NaN);
		
		mTimeStepDuration = timeStepDuration;		
	}
	
	public void setTimeStepsCount(int timeStepsCount)
	{
		assert(timeStepsCount > 0);
		
		mTimeStepsCount = timeStepsCount;
	}
	
	public void setTaxiAgentsData(List<AgentData> agentsData)
	{
		assert(agentsData != null);
		
		mTaxiAgentsData = agentsData;
	}
	
	public void setUserAgentsData(List<AgentData> agentsData)
	{
		assert(agentsData != null);
		
		mUserAgentsData = agentsData;
	}
	
	private void addAgentsToScene(ObservableList<Node> rootChildren)
	{
		for(AgentData agentData: mTaxiAgentsData)
		{
			Location startLocation = agentData.getLocations().remove(0);
			Shape agentShape = getTaxiShape(startLocation);
			Label label = new Label(agentData.getName());
			HBox hBox = new HBox(5);
			hBox.getChildren().addAll(agentShape, label);
			addAnimations(hBox, agentData);
			agentData.setNode(hBox);
			
			rootChildren.add(hBox);
		}
		
		for(AgentData agentData: mUserAgentsData)
		{
			Location startLocation = agentData.getLocations().remove(0);
			Shape agentShape = getUserShape(startLocation);
			Label label = new Label(agentData.getName());
			VBox vBox = new VBox(5);
			vBox.getChildren().addAll(agentShape, label);
			addAnimations(vBox, agentData);
			agentData.setNode(vBox);
			
			rootChildren.add(vBox);
		}
	}
	
	private Shape getTaxiShape(Location location)
	{
		double agentX = location.getX() * PIXELS_PER_AREA_UNIT;
		double agentY = location.getY() * PIXELS_PER_AREA_UNIT;
		
		Color taxiCabColor = new Color(	0.94901960784313725490196078431373, 
										0.89803921568627450980392156862745, 
										0.11372549019607843137254901960784, 
										1);
		Circle circle = CircleBuilder.create().
				centerX(agentX).centerY(agentY).
				radius(TAXI_SIZE).
				fill(taxiCabColor).
				build();
		
		return circle;
	}
	
	private Shape getUserShape(Location location)
	{
		double agentX = location.getX() * PIXELS_PER_AREA_UNIT;
		double agentY = location.getY() * PIXELS_PER_AREA_UNIT;
		
		Polygon triangle = PolygonBuilder.create().
				points(new Double[] 
				{
						agentX, agentY,
						agentX + USER_SIZE / 2, agentY + USER_SIZE,
						agentX - USER_SIZE / 2, agentY + USER_SIZE,
				}).
				fill(Color.GREEN).
				build();
				
//				CircleBuilder.create().
//				centerX(agentX).centerY(agentY).
//				radius(USER_SIZE).
//				fill(new Color(0, 0, 0, 1)).
//				build();
//		
		return triangle;
	}
	
	private void addAnimations(final Node agentNode, AgentData agentData)
	{
		final String agentName = agentData.getName();
		double currentX = agentNode.getLayoutX();
		double currentY = agentNode.getLayoutY();

		System.out.println("Plotting path for " + agentName + 
				", startX " + currentX + ", startY " + currentX);

		SequentialTransition sequentialTransition = new SequentialTransition();
		agentData.setAnimation(sequentialTransition);
		ArrayList<Location> locations = agentData.getLocations();
		for(Location location: locations)
		{
			double nextX = location.getX() * PIXELS_PER_AREA_UNIT;
			double nextY = location.getY() * PIXELS_PER_AREA_UNIT;
			
			Transition transition;
			if((nextX == currentX) && (nextY == currentY))
			{
				transition = new PauseTransition(mTimeStepDuration);
			}
			else
			{
				System.out.println("Plotting path for " + agentName + 
						", nextX " + nextX + ", nextY " + nextY);
				transition = TranslateTransitionBuilder.create().
						node(agentNode).duration(mTimeStepDuration).
						fromX(currentX).toX(nextX).
						fromY(currentY).toY(nextY).
						cycleCount(1).autoReverse(false).
						build();
				currentX = nextX;
				currentY = nextY;
			}
			sequentialTransition.getChildren().add(transition);
		}
		sequentialTransition.setCycleCount(1);
		sequentialTransition.setAutoReverse(false);
		sequentialTransition.setOnFinished(new EventHandler<ActionEvent>() 
		{
			@Override
			public void handle(ActionEvent event) 
			{
				System.out.println("Animation completed for " + agentName);
				
				mAgentsAnimatingCount--;
				agentNode.setOpacity(0.2);
				if(mAgentsAnimatingCount == 0)
				{
					System.out.println("Animation finished!");
					
					// replay();
				}
			}
		});
		sequentialTransition.play();
		mAgentsAnimatingCount++;
	}
	
	private void replay()
	{
		Label simOverLabel = new Label("Replay");
		mRoot.getChildren().add(simOverLabel);
		
		for(AgentData agentData: mTaxiAgentsData)
		{
			agentData.getNode().setOpacity(1);
			agentData.getAnimation().playFromStart();
		}
		for(AgentData agentData: mUserAgentsData)
		{
			agentData.getNode().setOpacity(1);
			agentData.getAnimation().playFromStart();
		}
	}
}
