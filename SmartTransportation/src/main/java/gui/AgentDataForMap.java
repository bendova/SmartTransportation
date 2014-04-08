package gui;

import java.util.ArrayList;
import java.util.List;

import SmartTransportation.Simulation.TransportMethodSpeed;

import javafx.animation.Animation;
import javafx.scene.Node;

import uk.ac.imperial.presage2.util.location.Location;
import util.movement.Movement;

public class AgentDataForMap 
{
	public enum AgentType
	{
		USER,
		TAXI_CAB,
		BUS
	}
	
	private final String LAYOUTS_PATH = "../layouts/";
	private final String TAXI_SHAPE_LAYOUT = LAYOUTS_PATH + "TaxiShape.fxml";
	private final String USER_SHAPE_LAYOUT = LAYOUTS_PATH + "UserShape.fxml";
	private final String BUS_SHAPE_LAYOUT = LAYOUTS_PATH + "BusShape.fxml";
	
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
			mLayoutPath = TAXI_SHAPE_LAYOUT;
			break;
		case USER:
			mLayoutPath = USER_SHAPE_LAYOUT;
			break;
		case BUS:
			mLayoutPath = BUS_SHAPE_LAYOUT;
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
