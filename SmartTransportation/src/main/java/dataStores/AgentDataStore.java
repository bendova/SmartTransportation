package dataStores;

import gui.agents.AgentDataForMap.AgentType;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;
import util.movement.Movement;

public class AgentDataStore 
{
	private String mName;
	private UUID mID;
	private AgentType mAgentType;
	private List<Movement> mMovements = null;
	private Location mStartLocation;
	
	public AgentDataStore(String agentName, UUID agentID, 
			AgentType agentType, Location startLocation)
	{
		assert(agentName != null);
		assert(agentID != null);
		assert(agentType != null);
		assert(startLocation != null);
		
		mName = agentName;
		mID = agentID;
		mAgentType = agentType;
		mStartLocation = startLocation;
	}
	public String getName()
	{
		return mName;
	}
	public UUID getID()
	{
		return mID;
	}
	public AgentType getAgentType()
	{
		return mAgentType;
	}
	public void setMovements(List<Movement> movements)
	{
		mMovements = movements;
	}
	public List<Movement> getMovements()
	{
		return mMovements;
	}
	public Location getStartLocation()
	{
		return mStartLocation;
	}
}
