package gui.agents;

import gui.GUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.FadeTransitionBuilder;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransitionBuilder;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import javafx.util.Callback;
import javafx.util.Duration;
import uk.ac.imperial.presage2.util.location.Location;
import util.movement.Movement;

public class AgentDataForMap 
{
	public enum AgentType
	{
		USER("User"),
		TAXI_CAB("Taxi Cab"),
		BUS("Bus");
		
		private String mTypeDescription;
		private AgentType(String description)
		{
			mTypeDescription = description;
		}
		public String getTypeDescription()
		{
			return mTypeDescription;
		}
	}
	
	private static final String SHAPES_PATH = GUI.LAYOUTS_PATH + "agentShapes/";

	public static final String USER_LAYOUT = SHAPES_PATH + "User.fxml";
	public static final String TAXI_LAYOUT = SHAPES_PATH + "Taxi.fxml";
	public static final String BUS_LAYOUT = SHAPES_PATH + "Bus.fxml";

	public static final String USER_SHAPE_LAYOUT = SHAPES_PATH + "UserShape.fxml";
	public static final String TAXI_SHAPE_LAYOUT = SHAPES_PATH + "TaxiShape.fxml";
	public static final String BUS_SHAPE_LAYOUT = SHAPES_PATH + "BusShape.fxml";
	
	private String mName;
	private List<Movement> mMovements;
	private Node mNode;
	private SequentialTransition mAnimation;
	private String mLayoutPath;
	private AgentType mAgentType;
	private Location mStartLocation;
	
	private double mPixelsPerAreaPoint;
    private Polyline mPathLine;
    private boolean mIsPathShowing;
    private Duration mTimeStepDuration;
    private EventHandler<ActionEvent> mOnAnimationFinished;
    private boolean mIsNodeAddedToRoot = false;
    
    private ObservableList<Node> mRootChildrenList;
	
	public AgentDataForMap(AgentType type, String name, List<Movement> movements,
			Location startLocation, double pixelsPerAreaPoint, Duration duration)
	{
		assert(type != null);
		assert(name != null);
		assert(startLocation != null);
		assert(pixelsPerAreaPoint > 0);
		assert(duration != null);
		
		mName = name;
		mAgentType = type;
		mStartLocation = startLocation;
		mPixelsPerAreaPoint = pixelsPerAreaPoint;
		mIsPathShowing = false;
		mTimeStepDuration = duration;
		mAnimation = new SequentialTransition();
		
		if(movements != null)
		{
			mMovements = movements;
		}
		else
		{
			mMovements = new ArrayList<Movement>(0);
		}
		
		switch (mAgentType) 
		{
		case TAXI_CAB:
			mLayoutPath = TAXI_LAYOUT;
			break;
		case USER:
			mLayoutPath = USER_LAYOUT;
			break;
		case BUS:
			mLayoutPath = BUS_LAYOUT;
			break;
		default:
			assert(false) : "Case not handled for mAgentType " + mAgentType;
			break;
		}
		setNode(loadAgentNode(mLayoutPath, mName));
	}
	
	public void setRootChildrenList(ObservableList<Node> rootChildrenList)
	{
		mRootChildrenList = rootChildrenList;
		addNodeToRoot();
	}
	
	public void setOnAnimationFinished(EventHandler<ActionEvent> onAnimationFinished)
	{
		mOnAnimationFinished = onAnimationFinished;
		if(mAnimation != null)
		{
			mAnimation.setOnFinished(mOnAnimationFinished);
		}
	}
	
	public void playAnimation()
	{
		if(mAnimation.getTotalDuration().greaterThan(mAnimation.getCurrentTime()))
		{
			mAnimation.play();
		}
	}
	
	public void pauseAnimation()
	{
		mAnimation.pause();
	}
	
	public void restartAnimation()
	{
		mAnimation.playFromStart();
	}
	
	public void jumpToInAnimation(Duration position)
	{
		if(position != null)
		{
			mAnimation.pause();
			mAnimation.jumpTo(position);
		}
	}
	
	public AgentType getAgentType()
	{
		return mAgentType;
	}
	
	private void setNode(AgentNodeController nodeController)
	{
		if(nodeController != null)
		{
			mNode = nodeController.getNode();
			addNodeToRoot();
			
			createAnimation();
			createPathLine();
		}
	}
	
	private void createAnimation()
	{
		double currentX = mStartLocation.getX() * mPixelsPerAreaPoint;
		double currentY = mStartLocation.getY() * mPixelsPerAreaPoint;

		Transition startTransition = TranslateTransitionBuilder.create().
				node(mNode).duration(Duration.millis(100)).
				fromX(0).toX(currentX).
				fromY(0).toY(currentY).
				cycleCount(1).autoReverse(false).
				interpolator(Interpolator.LINEAR).
				build();
		mAnimation.getChildren().add(startTransition);
		
		FadeTransition fadeInTransition = FadeTransitionBuilder.create().
				node(mNode).duration(Duration.millis(100)).
				toValue(1).
				build();
		mAnimation.getChildren().add(fadeInTransition);
		
		int currentTime = 0;
		for(Movement movement: mMovements)
		{
			int movementStartTime = movement.getStartTime();
			
			if(currentTime < movementStartTime)
			{
				Transition pauseTransition = new PauseTransition(mTimeStepDuration.
						multiply((movementStartTime - currentTime)));
				currentTime = movementStartTime;
				mAnimation.getChildren().add(pauseTransition);
			}
			
			Location location = movement.getLocation();
			double nextX = location.getX() * mPixelsPerAreaPoint;
			double nextY = location.getY() * mPixelsPerAreaPoint;
			
			int timeTakenPerUnitDistance = movement.getTimeTakenPerUnitDistance();
			currentTime += timeTakenPerUnitDistance;
			
			Transition moveTransition = TranslateTransitionBuilder.create().
					node(mNode).duration(mTimeStepDuration.multiply(timeTakenPerUnitDistance)).
					fromX(currentX).toX(nextX).
					fromY(currentY).toY(nextY).
					interpolator(Interpolator.LINEAR).
					cycleCount(1).autoReverse(false).
					build();
			currentX = nextX;
			currentY = nextY;
			mAnimation.getChildren().add(moveTransition);
		}
		FadeTransition fadeOutTransition = FadeTransitionBuilder.create().
				node(mNode).duration(Duration.millis(100)).
				toValue(0.2).
				build();
		
		mAnimation.getChildren().add(fadeOutTransition);
		
		mAnimation.setCycleCount(1);
		mAnimation.setAutoReverse(false);
		mAnimation.setInterpolator(Interpolator.LINEAR);
		if(mOnAnimationFinished != null)
		{
			mAnimation.setOnFinished(mOnAnimationFinished);
		}
	}
	
	private void createPathLine()
	{
        mPathLine = new Polyline();
		ObservableList<Double> linePoints = mPathLine.getPoints();
		final double CENTER_OFFSET = 0.5;
		linePoints.add((mStartLocation.getX() + CENTER_OFFSET) * mPixelsPerAreaPoint);
		linePoints.add((mStartLocation.getY() + CENTER_OFFSET) * mPixelsPerAreaPoint);
		for (Iterator<Movement> iterator = mMovements.iterator(); iterator.hasNext();)
		{
			Location location = iterator.next().getLocation();
			linePoints.add((location.getX() + CENTER_OFFSET) * mPixelsPerAreaPoint);
			linePoints.add((location.getY() + CENTER_OFFSET)* mPixelsPerAreaPoint);
		}
		mPathLine.setStroke(getColor());
		mPathLine.setStrokeWidth(1d);
		mPathLine.getStrokeDashArray().addAll(10d, 10d);
		mPathLine.setFill(null);
    }
	
	private Paint getColor()
	{
		switch (mAgentType)
		{
		case USER:
			return Color.GREEN;
		case TAXI_CAB:
			return Color.DARKORANGE;
		case BUS:
			return Color.BLUE;
		default:
			return Color.RED;
		}
	}
	
	private void togglePath()
	{
		if(mIsPathShowing)
		{
			mRootChildrenList.remove(mPathLine);
		}
		else 
		{
			mRootChildrenList.add(mPathLine);
		}
		mIsPathShowing = !mIsPathShowing;
	}
	
	private AgentNodeController loadAgentNode(String path, String name)
	{
		AgentNodeController controller = (AgentNodeController)
				(GUI.loadNode(path)).getController();
		controller.setTitle(name);
		controller.setOnClickCallback(new Callback<Void, Void>()
		{
			@Override
			public Void call(Void param)
			{
				togglePath();
				return null;
			}
		});
		return controller;
	}
	
	private void addNodeToRoot()
	{
		if((mIsNodeAddedToRoot == false) && (mNode != null) && 
			(mRootChildrenList != null))
		{
			mRootChildrenList.add(mNode);
			mIsNodeAddedToRoot = true;
		}
	}
}
