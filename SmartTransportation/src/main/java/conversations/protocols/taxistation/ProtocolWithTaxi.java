package conversations.protocols.taxistation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import conversations.commonActions.SendMessageAction;
import conversations.taxiStationTaxi.ConversationDescription;
import conversations.taxiStationTaxi.actions.OnOrderCompleteAction;
import conversations.taxiStationTaxi.actions.OnRegisterAction;
import conversations.taxiStationTaxi.actions.OnRejectOrderAction;
import conversations.taxiStationTaxi.actions.OnTaxiStatusUpdateAction;
import conversations.taxiStationTaxi.messages.RegisterAsTaxiMessage;
import conversations.taxiStationTaxi.messages.RejectOrderMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderCompleteMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderMessage;
import conversations.taxiStationTaxi.messages.TaxiStatusUpdateMessage;
import uk.ac.imperial.presage2.core.network.NetworkAdaptor;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.UnicastMessage;
import util.protocols.FSMProtocol;

public class ProtocolWithTaxi 
{
	private NetworkAdaptor mAdaptor;
	private FSMProtocol mWithTaxiProtocol;
	private Map<NetworkAddress, UUID> conversationsMap = 
			new HashMap<NetworkAddress, UUID>();
	
	public ProtocolWithTaxi(NetworkAdaptor networkAdaptor)
	{
		assert(networkAdaptor != null);
		
		mAdaptor = networkAdaptor;
	}
	
	public void init(	OnRegisterAction 			onRegisterAction,
						OnOrderCompleteAction 		onOrderCompleteAction,
						OnRejectOrderAction 		onRejectOrderAction,
						OnTaxiStatusUpdateAction 	onTaxiStatusUpdateAction)
	{
		ConversationDescription description = new ConversationDescription();
		
		SendMessageAction onSendOrderAction = new SendMessageAction();
		description.init(onRegisterAction, onSendOrderAction, onOrderCompleteAction, 
				onRejectOrderAction, onTaxiStatusUpdateAction, onTaxiStatusUpdateAction);
		
		mWithTaxiProtocol = new FSMProtocol(ConversationDescription.PROTOCOL_NAME, description, mAdaptor);
	}
	
	public void handleRegisterTaxiMessage(RegisterAsTaxiMessage msg)
	{
		conversationsMap.put(msg.getFrom(), msg.getConversationKey());
		mWithTaxiProtocol.spawn(msg);
	}
	
	public void sendOrder(TaxiOrderMessage msg)
	{
		sendMessage(msg);
	}
	
	public void handleOrderComplete(TaxiOrderCompleteMessage msg)
	{
		mWithTaxiProtocol.handle(msg);
	}
	
	public void handleOrderReject(RejectOrderMessage msg)
	{
		mWithTaxiProtocol.handle(msg);
	}
	
	public void handleTaxiStatusUpdate(TaxiStatusUpdateMessage msg)
	{
		mWithTaxiProtocol.handle(msg);
	}
	
	private void sendMessage(UnicastMessage<?> msg)
	{
		msg.setConversationKey(conversationsMap.get(msg.getTo()));
		msg.setProtocol(ConversationDescription.PROTOCOL_NAME);
		mWithTaxiProtocol.handle(msg);
	}
}
