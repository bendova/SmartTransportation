package dataStores;

public class AgentMoveData
{
	public enum MoveDataType
	{
		MOVEMENT,
		PAUSE
	}
	private MoveData mMoveData = null;
	private MoveDataType mMoveDataType;
	
	public AgentMoveData()
	{
		mMoveDataType = MoveDataType.PAUSE;
	}
	public AgentMoveData(MoveData moveData)
	{
		assert(moveData != null);
		
		mMoveData = moveData;
		mMoveDataType = MoveDataType.MOVEMENT;
	}
	
	public MoveData getMoveData()
	{
		return mMoveData;
	}
	public MoveDataType getMoveDataType()
	{
		return mMoveDataType;
	}
}
