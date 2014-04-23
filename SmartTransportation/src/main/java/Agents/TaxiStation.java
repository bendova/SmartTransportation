package agents;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import map.CityMap;

import conversations.protocols.taxistation.ProtocolWithTaxi;
import conversations.taxiStationMediator.RegisterAsTaxiStationMessage;
import conversations.taxiStationMediator.TaxiAvailableMessage;
import conversations.taxiStationMediator.messageData.ITaxiDescription;
import conversations.taxiStationMediator.messageData.ITaxiServiceRequest;
import conversations.taxiStationMediator.messageData.TaxiDescription;
import conversations.taxiStationTaxi.actions.OnOrderCompleteAction;
import conversations.taxiStationTaxi.actions.OnRegisterAction;
import conversations.taxiStationTaxi.actions.OnRejectOrderAction;
import conversations.taxiStationTaxi.actions.OnTaxiStatusUpdateAction;
import conversations.taxiStationTaxi.messages.RegisterAsTaxiMessage;
import conversations.taxiStationTaxi.messages.RejectOrderMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderCompleteMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderMessage;
import conversations.taxiStationTaxi.messages.TaxiStatusUpdateMessage;
import conversations.userTaxi.messages.TaxiReplyMessage;
import conversations.userTaxi.messages.TaxiRequestCancelMessage;
import conversations.userTaxi.messages.TaxiRequestConfirmationMessage;
import conversations.userTaxi.messages.messageData.TaxiData;
import conversations.userTaxi.messages.messageData.TaxiOrder;
import conversations.userTaxi.messages.messageData.TaxiServiceReply;
import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;


public class TaxiStation extends AbstractParticipant
{
	private enum TaxiRequestState
	{
		READY_FOR_SERVICE,
		BEING_SERVICED,
		COMPLETED,
		CANCELED
	}
	
	private class TaxiRequest implements TimeDriven, Comparable<TaxiRequest>
	{
		private TaxiRequestState mCurrentState;
		private ITaxiServiceRequest mRequestData;
		private NetworkAddress mFromUserAddress;
		private NetworkAddress mServicedByTaxi;
		
		private int mCurrentTime;
		
		public TaxiRequest(TaxiRequestConfirmationMessage requestMessage)
		{
			assert(requestMessage != null);
			
			mRequestData = requestMessage.getData();
			mServicedByTaxi = mRequestData.getTaxiDescription().getTaxiAddress();
			mFromUserAddress = mRequestData.getUserNetworkAddress();
			mCurrentState = TaxiRequestState.READY_FOR_SERVICE;
			
			mCurrentTime = 0;
		}
		
		public ITaxiServiceRequest getRequestData()
		{
			return mRequestData;
		}
		
		public NetworkAddress getFrom()
		{
			return mFromUserAddress;
		}
		
		public TaxiRequestState getCurrentState()
		{
			return mCurrentState;
		}
		
		public NetworkAddress getServicedBy()
		{
			return mServicedByTaxi;
		}
		
		public void setAsBeingServiced()
		{
			assert(mCurrentState == TaxiRequestState.READY_FOR_SERVICE);
			
			mCurrentState = TaxiRequestState.BEING_SERVICED;
		}
		
		public void setAsCompleted()
		{
			if(mCurrentState != TaxiRequestState.BEING_SERVICED)
			{
				logger.info("setAsCompleted() Logic error!");
			}
			assert(mCurrentState == TaxiRequestState.BEING_SERVICED);
			
			mCurrentState = TaxiRequestState.COMPLETED;
		}
		
		public void setAsCanceled()
		{
			assert(mCurrentState != TaxiRequestState.BEING_SERVICED);
			assert(mCurrentState != TaxiRequestState.COMPLETED);
			
			mCurrentState = TaxiRequestState.CANCELED;
		}

		@Override
		public void incrementTime() 
		{
			mCurrentTime++;
		}
		
		@Override
		public int compareTo(TaxiRequest o) 
		{
			if(o != null)
			{
				return o.mCurrentTime - this.mCurrentTime;
			}
			return 1;
		}
	}
	
	private Location mLocation;
	private CityMap mCityMap;
	private NetworkAddress mMediatorAddress;
	private Queue<TaxiRequest> mTaxiRequests;
	private Map<NetworkAddress, UUID> mTaxiesMap;
	private Set<NetworkAddress> mAvailableTaxiesSet;
	private ParticipantLocationService mLocationService;
	private ProtocolWithTaxi withTaxi;
	
	public TaxiStation(UUID id, String name, Location location, CityMap cityMap, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mCityMap = cityMap;
		mMediatorAddress = mediatorNetworkAddress;
		
		mTaxiesMap = new HashMap<NetworkAddress, UUID>();
		mAvailableTaxiesSet = new HashSet<NetworkAddress>();
		mTaxiRequests = new ConcurrentLinkedQueue<TaxiRequest>();
	}
	
	public NetworkAddress getNetworkAddress() 
	{
		return network.getAddress();
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return ss;
	}
	
	@Override
	public void initialise()
	{
		super.initialise();
		
		registerAsTaxiServiceProvider();
		initializeLocationService();
		initializeProtocols();
	}
	
	private void registerAsTaxiServiceProvider()
	{
		RegisterAsTaxiStationMessage submitMessage = new 
				RegisterAsTaxiStationMessage(network.getAddress(), mMediatorAddress);
		network.sendMessage(submitMessage);
	}
	
	private void initializeLocationService()
	{
		try
		{
			mLocationService = getEnvironmentService(ParticipantLocationService.class);
		}
		catch (UnavailableServiceException e) 
		{
			logger.warn(e);
		}
	}
	
	private void initializeProtocols()
	{
		withTaxi = new ProtocolWithTaxi(network);
		OnRegisterAction onRegisterAction = new OnRegisterAction() 
		{
			@Override
			public void processMessage(RegisterAsTaxiMessage msg) 
			{
				registerTaxi(msg);
			}
		};
		
		OnOrderCompleteAction onOrderCompleteAction = new OnOrderCompleteAction() 
		{
			@Override
			public void processMessage(TaxiOrderCompleteMessage msg) 
			{
				handleTaxiOrderComplete(msg);
			}
		};
		
		OnRejectOrderAction onRejectOrderAction = new OnRejectOrderAction() 
		{
			@Override
			public void processMessage(RejectOrderMessage msg) 
			{
				handleTaxiOrderRejected(msg);
			}
		};
		
		OnTaxiStatusUpdateAction onTaxiStatusUpdateAction = new OnTaxiStatusUpdateAction() 
		{
			@Override
			public void processMessage(TaxiStatusUpdateMessage msg) 
			{
				handleTaxiStatusUpdate(msg);
			}
		};
		withTaxi.init(onRegisterAction, onOrderCompleteAction, onRejectOrderAction, onTaxiStatusUpdateAction);
	}
	
	@Override
	protected void processInput(Input input) 
	{
		logger.info("processInput() " + input);
		
		if(input != null)
		{
			if (input instanceof TaxiRequestConfirmationMessage)
			{
				processRequestConfirmation((TaxiRequestConfirmationMessage)input);
			}
			else if(input instanceof TaxiRequestCancelMessage)
			{
				processRequestCancel((TaxiRequestCancelMessage)input);
			}
			else if (input instanceof RegisterAsTaxiMessage)
			{
				withTaxi.handleRegisterTaxiMessage((RegisterAsTaxiMessage)input);
			}
			else if (input instanceof TaxiOrderCompleteMessage)
			{
				withTaxi.handleOrderComplete((TaxiOrderCompleteMessage)input);
			}
			else if (input instanceof TaxiStatusUpdateMessage)
			{
				withTaxi.handleTaxiStatusUpdate((TaxiStatusUpdateMessage)input);
			}
			else if(input instanceof RejectOrderMessage)
			{
				withTaxi.handleOrderReject((RejectOrderMessage)input);
			}
		}
	}
	
	private void processRequestConfirmation(TaxiRequestConfirmationMessage requestConfirmationMessage)
	{
		logger.info("processRequestConfirmation() requestConfirmationMessage " + requestConfirmationMessage);
		
		TaxiRequest taxiRequest = new TaxiRequest(requestConfirmationMessage);
		mTaxiRequests.add(taxiRequest);
		mAvailableTaxiesSet.remove(taxiRequest.getServicedBy());
	}
	
	private void processRequestCancel(TaxiRequestCancelMessage requestCancel)
	{
		logger.info("processRequestCancel() " + requestCancel);
		
		for(TaxiRequest taxiRequest : mTaxiRequests)
		{
			if(taxiRequest.getFrom().equals(requestCancel.getFrom()))
			{
				if(taxiRequest.getCurrentState() == TaxiRequestState.CANCELED)
				{
					return;
				}
				
				setTaxiAsAvailable(taxiRequest.getServicedBy());
				taxiRequest.setAsCanceled();
				break;
			}
		}
	}
	
	private void registerTaxi(RegisterAsTaxiMessage taxiRegisterMessage)
	{
		logger.info("registerTaxi() taxiRegisterMessage " + taxiRegisterMessage);
		
		TaxiData newTaxiData = taxiRegisterMessage.getData();
		if(newTaxiData != null)
		{
			NetworkAddress taxiAddress = newTaxiData.getNetworkAddress();
			mTaxiesMap.put(taxiAddress, newTaxiData.getID());
			setTaxiAsAvailable(taxiAddress);
		}
	}
	
	private void handleTaxiOrderComplete(TaxiOrderCompleteMessage taxiOrderCompleteMessage)
	{
		logger.info("handleTaxiOrderComplete() taxiOrderCompleteMessage " + taxiOrderCompleteMessage);
		
		NetworkAddress reportingTaxiAddress = taxiOrderCompleteMessage.getFrom();
		if(mTaxiesMap.containsKey(reportingTaxiAddress))
		{
			if(mAvailableTaxiesSet.contains(reportingTaxiAddress))
			{
				logger.info("handleTaxiOrderComplete() Logic error!");
			}
			
			assert(mAvailableTaxiesSet.contains(reportingTaxiAddress) == false);
			setTaxiAsAvailable(reportingTaxiAddress);
			
			for(TaxiRequest taxiRequest : mTaxiRequests)
			{
				NetworkAddress taxiAddress = taxiRequest.getServicedBy();
				if((taxiAddress != null) && taxiAddress.equals(reportingTaxiAddress))
				{
					logger.info("handleTaxiOrderComplete() taxiRequest " + taxiRequest);
					taxiRequest.setAsCompleted();
					break;
				}
			}
		}
	}
	
	private void setTaxiAsAvailable(NetworkAddress taxiAddress)
	{
		mAvailableTaxiesSet.add(taxiAddress);
		offerTaxiToMediator(taxiAddress);
	}
	
	private void offerTaxiToMediator(NetworkAddress taxiAddress)
	{
		Location taxiLocation = mLocationService.getAgentLocation(taxiAddress.getId());
		ITaxiDescription taxiDescription = new TaxiDescription(taxiAddress, taxiLocation, 
				getNetworkAddress());
		TaxiAvailableMessage msg = new TaxiAvailableMessage(taxiDescription, 
				getNetworkAddress(), mMediatorAddress);
		network.sendMessage(msg);
	}
	
	private void handleTaxiStatusUpdate(TaxiStatusUpdateMessage msg)
	{
		logger.info("handleTaxiStatusUpdate() msg " + msg);
		
		NetworkAddress reportingTaxiAddress = msg.getFrom();
		
		if(mTaxiesMap.containsKey(reportingTaxiAddress))
		{
			switch(msg.getData())
			{
			case AVAILABLE:
				assert(mAvailableTaxiesSet.contains(reportingTaxiAddress) == false) : 
					"Taxi " + msg.getFrom() + " should not already be in our free taxies list!";
				
				logger.info("handleTaxiStatusUpdate() adding to mFreeTaxiesList " + msg.getFrom());
				setTaxiAsAvailable(reportingTaxiAddress);
				break;
			case IN_REVISION:
			case BROKEN:
				if (mAvailableTaxiesSet.contains(reportingTaxiAddress))
				{
					logger.info("handleTaxiStatusUpdate() removing from mFreeTaxiesList " + msg.getFrom());
					
					mAvailableTaxiesSet.remove(reportingTaxiAddress);
					// TODO notify mediator that this taxi is no longer available
				}
				else
				{
					logger.info("handleTaxiStatusUpdate() mTaxiesMap.get(reportingTaxiAddress) " + mTaxiesMap.get(reportingTaxiAddress));
					
					// TODO haha - notify Mediator that the taxi has broken down,
					// and have him deal with it
				}
				break;
			default:
				assert(false);
			}
		}
	}
	
	private void handleTaxiOrderRejected(RejectOrderMessage msg)
	{
		logger.info("handleTaxiOrderRejected() msg " + msg);
		
		NetworkAddress reportingTaxiAddress = msg.getFrom();
		if(mTaxiesMap.containsKey(reportingTaxiAddress))
		{
			for(TaxiRequest taxiRequest : mTaxiRequests)
			{
				NetworkAddress taxiAddress = taxiRequest.getServicedBy();
				if((taxiAddress != null) && (taxiAddress.equals(reportingTaxiAddress)))
				{
					// FIXME taxiRequest.setAsPendingProcessing();
					taxiRequest.setAsCanceled();
					break;
				}
			}
		}
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		for (TaxiRequest taxiRequest : mTaxiRequests) 
		{
			taxiRequest.incrementTime();
		}
		
		processRequests();
	}
	
	private void processRequests()
	{
		for (TaxiRequest taxiRequest : mTaxiRequests) 
		{
			switch(taxiRequest.getCurrentState())
			{
				case READY_FOR_SERVICE:
					logger.info("processRequests() confirmed request! ");
					handleConfirmedRequest(taxiRequest);
					break;
				case BEING_SERVICED:
					// do nothing
					break;
				case CANCELED:
				case COMPLETED:
					// TODO add to statistics
					mTaxiRequests.remove(taxiRequest);
					break;
			}
		}
	}
	
	private void handleConfirmedRequest(TaxiRequest taxiRequest)
	{
		logger.info("handleConfirmedRequest() taxiRequest " + taxiRequest);
		
		NetworkAddress taxiAddress = taxiRequest.getServicedBy();
		UUID taxiID = mTaxiesMap.get(taxiAddress);
		sendReplyToUser(taxiRequest.getFrom(), taxiID);
		
		sendOrderToTaxi(taxiAddress, taxiRequest.getRequestData(), taxiRequest.getFrom());
		taxiRequest.setAsBeingServiced();
	}
	
	private void sendReplyToUser(NetworkAddress toUser, UUID taxiID)
	{
		logger.info("SendReplyToUser() toUser " + toUser);
		
		TaxiServiceReply taxiServiceReply = new TaxiServiceReply(taxiID, "A taxi will be comming your way!");
		TaxiReplyMessage taxiServiceReplyMessage = 
				new TaxiReplyMessage(taxiServiceReply, network.getAddress(), toUser);
		network.sendMessage(taxiServiceReplyMessage);
	}
	
	private void sendOrderToTaxi(NetworkAddress toTaxi, ITaxiServiceRequest request, NetworkAddress userNetworkAddress)
	{
		logger.info("SendOrderToTaxi() toTaxi " + toTaxi);
		
		UUID userID = request.getUserID();
		UUID authKey = request.getUserAuthKey();
		Location userLocation = request.getStartLocation();
		TaxiOrder taxiOrder = new TaxiOrder(userLocation, userID, authKey, userNetworkAddress);
		TaxiOrderMessage taxiOrderMessage = new TaxiOrderMessage(taxiOrder, network.getAddress(), toTaxi);
		withTaxi.sendOrder(taxiOrderMessage);
	}
}
