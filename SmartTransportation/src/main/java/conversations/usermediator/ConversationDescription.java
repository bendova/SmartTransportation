package conversations.usermediator;

import util.protocols.*;

// TODO This conversation should go like this:
// User sends a request to Mediator
// Mediator replies with ...

public class ConversationDescription extends FSMDescription
{
	public static final String PROTOCOL_NAME = "UserWithMediatorConvesationProtocol";
	
	private static final String STATE_WAITING_REQUEST = "WaitingRequest";
}
