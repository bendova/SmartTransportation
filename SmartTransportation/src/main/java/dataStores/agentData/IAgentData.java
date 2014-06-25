package dataStores.agentData;

import gui.agents.AgentDataForMap.AgentType;

import java.util.List;
import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;
import util.movement.Movement;

public interface IAgentData
{
	String getName();
	UUID getID();
	AgentType getAgentType();
	void setMovements(List<Movement> movements);
	List<Movement> getMovements();
	Location getStartLocation();
}
