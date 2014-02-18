package gui;

import java.util.List;

import javafx.animation.Animation;
import javafx.scene.Node;

import uk.ac.imperial.presage2.util.location.Location;

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
	private List<Location> mLocations;
	private Node mNode;
	private Animation mAnimation;
	private String mLayoutPath;
	private AgentType mAgentType;
	
	public AgentDataForMap(AgentType type, String name, List<Location> locations)
	{
		assert(name != null);
		assert(locations != null);
		
		mName = name;
		mAgentType = type;
		mLocations = locations;
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
			break;
		}
	}
	
	public String getName() 
	{
		return mName; 
	}
	
	public List<Location> getLocations() 
	{
		return mLocations;
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
}
