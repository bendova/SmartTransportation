package dataStores.agentData;

import java.util.UUID;

public interface IAgentDataStore
{
	void addAgentData(UUID agentID, IAgentData agentDataStore);
}
