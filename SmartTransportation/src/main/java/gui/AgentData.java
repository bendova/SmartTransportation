package gui;

import java.util.ArrayList;

import javafx.animation.Animation;
import javafx.scene.Node;
import javafx.scene.shape.Shape;

import uk.ac.imperial.presage2.util.location.Location;

public class AgentData 
{
	public enum AgentType
	{
		TAXI_USER,
		TAXI_CAB
	}
	
	private String mName;
	private ArrayList<Location> mLocations;
	private Node mNode;
	private Animation mAnimation;
	private AgentType mAgentType;
	
	public AgentData(AgentType type, String name, ArrayList<Location> locations)
	{
		assert(name != null);
		assert(locations != null);
		
		mName = name;
		mAgentType = type;
		mLocations = locations;
	}
	
	public String getName() 
	{
		return mName; 
	}
	
	public ArrayList<Location> getLocations() 
	{
		return mLocations;
	}
	
	public AgentType getType()
	{
		return mAgentType;
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
