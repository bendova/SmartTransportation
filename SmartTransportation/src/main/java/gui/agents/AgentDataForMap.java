package gui.agents;

import gui.GUI;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.scene.Node;

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
	private Animation mAnimation;
	private String mLayoutPath;
	private AgentType mAgentType;
	private Location mStartLocation;
	
	public AgentDataForMap(AgentType type, String name, List<Movement> movements,
			Location startLocation)
	{
		assert(type != null);
		assert(name != null);
		assert(startLocation != null);
		
		mName = name;
		mAgentType = type;
		mStartLocation = startLocation;
		
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
	}
	
	public String getName() 
	{
		return mName; 
	}
	
	public List<Movement> getMoveData() 
	{
		return mMovements;
	}
	
	public AgentType getType()
	{
		return mAgentType;
	}
	
	public String getLayoutPath()
	{
		return mLayoutPath;
	}
	
	public void setNode(Node node)
	{
		assert(node != null);
		
		mNode = node;
	}
	
	public Node getNode()
	{
		return mNode;
	}
	
	public void setAnimation(Animation animation)
	{
		assert(animation != null);
		
		mAnimation = animation;
	}
	
	public Animation getAnimation()
	{
		return mAnimation;
	}
	
	public Location getStartLocation()
	{
		return mStartLocation;
	}
}
