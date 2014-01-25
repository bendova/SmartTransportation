package conversations.userBus.messages.messageData;

import java.util.UUID;

public class BoardBusRequest implements IBoardBusRequest
{
	private UUID mUserAuthKey;
	
	public BoardBusRequest(UUID authenticationKey)
	{
		assert(authenticationKey != null);
		
		mUserAuthKey = authenticationKey;
	}
	
	@Override
	public UUID getUserAuthKey()
	{
		return mUserAuthKey;
	}
}
