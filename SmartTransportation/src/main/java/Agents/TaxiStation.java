package agents;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import conversations.protocols.taxistation.ProtocolWithTaxi;
import conversations.taxiStationMediator.RegisterAsTaxiStationMessage;
import conversations.taxiStationTaxi.actions.OnOrderCompleteAction;
import conversations.taxiStationTaxi.actions.OnRegisterAction;
import conversations.taxiStationTaxi.actions.OnRejectOrderAction;
import conversations.taxiStationTaxi.actions.OnTaxiStatusUpdateAction;
import conversations.taxiStationTaxi.messages.RegisterAsTaxiMessage;
import conversations.taxiStationTaxi.messages.RejectOrderMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderCompleteMessage;
import conversations.taxiStationTaxi.messages.TaxiOrderMessage;
import conversations.taxiStationTaxi.messages.TaxiStatusUpdateMessage;
import conversations.userMediator.messages.ITransportServiceRequest;
import conversations.userMediator.messages.TransportServiceRequestMessage;
import conversations.userTaxi.messages.RequestTaxiConfirmationMessage;
import conversations.userTaxi.messages.TaxiReplyMessage;
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
		PENDING_PROCESSING,
		AWAITING_CONFIRMATION,
		READY_FOR_SERVICE,
		BEING_SERVICED,
		COMPLETED,
		CANCELED
	}
	
	private class TaxiRequest implements TimeDriven, Comparable<TaxiRequest>
	{
		private static final int DEFAULT_TIME_OUT = 5000;
		
		private TaxiRequestState mCurrentState;
		private ITransportServiceRequest mRequestData;
		private NetworkAddress mFromUserAddress;
		private NetworkAddress mServicedByTaxi;
		
		private int mCurrentTime;
		private int mTimeoutTime;
		
		private boolean mWasConfirmed;
		private boolean mIsAssigned;
		
		public TaxiRequest(TransportServiceRequestMessage requestMessage)
		{
			this(requestMessage, 0, DEFAULT_TIME_OUT);
		}
		
		public TaxiRequest(TransportServiceRequestMessage requestMessage, int currentTime, int timeOutTimeSteps)
		{
			assert(requestMessage != null);
			
			mRequestData = requestMessage.getData();
			mFromUserAddress = requestMessage.getFrom();
			mCurrentState = TaxiRequestState.PENDING_PROCESSING;
			
			mCurrentTime = currentTime;
			mTimeoutTime = timeOutTimeSteps;
			
			mWasConfirmed = false;
			mIsAssigned = false;
		}
		
		public ITransportServiceRequest getRequestData()
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
		
		public void setServicedBy(NetworkAddress taxiAddress)
		{
			mServicedByTaxi = taxiAddress;
			mIsAssigned = (mServicedByTaxi != null);
		}
		
		public NetworkAddress getServicedBy()
		{
			return mServicedByTaxi;
		}
		
		public void setAsPendingProcessing()
		{
			mCurrentState = TaxiRequestState.PENDING_PROCESSING;
		}
		
		public void setAsAwaitingConfirmation()
		{
			assert(mCurrentState == TaxiRequestState.PENDING_PROCESSING);
			
			mCurrentState = TaxiRequestState.AWAITING_CONFIRMATION;
		}
		
		public void setAsReadyForService()
		{
			assert(mWasConfirmed);
			assert(mIsAssigned);
			assert(mCurrentState != TaxiRequestState.CANCELED);
			assert(mCurrentState != TaxiRequestState.COMPLETED);
			
			mCurrentState = TaxiRequestState.READY_FOR_SERVICE;
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
		
		public boolean hasTimedOut()
		{
			return (mCurrentTime > mTimeoutTime);
		}

		public void setIsConfirmed()
		{
			mWasConfirmed = true;
		}
		
		public boolean wasConfirmed()
		{
			return mWasConfirmed;
		}
		
		public boolean isAssigned()
		{
			return mIsAssigned;
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
	private NetworkAddress mMediatorAddress;
	private Queue<TaxiRequest> mTaxiRequests;
	private Map<NetworkAddress, UUID> mTaxiesMap;
	private Set<NetworkAddress> mFreeTaxiesSet;
	private ParticipantLocationService mLocationService;
	private ProtocolWithTaxi withTaxi;
	
	public TaxiStation(UUID id, String name, Location location, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mMediatorAddress = mediatorNetworkAddress;
		
		mTaxiesMap = new HashMap<NetworkAddress, UUID>();
		mFreeTaxiesSet = new HashSet<NetworkAddress>();
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
			if(input instanceof TransportServiceRequestMessage)
			{
				storeRequest((TransportServiceRequestMessage)input);
			}
			else if (input instanceof TaxiRequestConfirmationMessage)
			{
				processRequestConfirmation((TaxiRequestConfirmationMessage)input);
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
	
	private void storeRequest(TransportServiceRequestMessage taxiRequestMessage)
	{
		logger.info("storeRequest()");
		
		mTaxiRequests.add(new TaxiRequest(taxiRequestMessage));
	}
	
	private void processRequestConfirmation(TaxiRequestConfirmationMessage requestConfirmationMessage)
	{
		logger.info("processRequestConfirmation() requestConfirmationMessage " + requestConfirmationMessage);
		
		for(TaxiRequest taxiRequest : mTaxiRequests)
		{
			if(taxiRequest.getFrom().equals(requestConfirmationMessage.getFrom()))
			{
				if(taxiRequest.getCurrentState() == TaxiRequestState.CANCELED)
				{
					return;
				}
				
				taxiRequest.setIsConfirmed();
				if(taxiRequest.isAssigned())
				{
					taxiRequest.setAsReadyForService();
				}
				else 
				{
					taxiRequest.setAsPendingProcessing();
				}
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
			mFreeTaxiesSet.add(taxiAddress);
		}
	}
	
	private void handleTaxiOrderComplete(TaxiOrderCompleteMessage taxiOrderCompleteMessage)
	{
		logger.info("handleTaxiOrderComplete() taxiOrderCompleteMessage " + taxiOrderCompleteMessage);
		
		NetworkAddress reportingTaxiAddress = taxiOrderCompleteMessage.getFrom();
		if(mTaxiesMap.containsKey(reportingTaxiAddress))
		{
			if(mFreeTaxiesSet.contains(reportingTaxiAddress))
			{
				logger.info("handleTaxiOrderComplete() Logic error!");
			}
			
			assert(mFreeTaxiesSet.contains(reportingTaxiAddress) == false);
			mFreeTaxiesSet.add(reportingTaxiAddress);
			
			for(TaxiRequest taxiRequest : mTaxiRequests)
			{
				NetworkAddress taxiAddress = taxiRequest.getServicedBy();
				if((taxiAddress != null) && taxiAddress.equals(reportingTaxiAddress))
				{
					logger.info("handleTaxiOrderComplete() taxiRequest " + taxiRequest);
					taxiRequest.setAsCompleted();
					taxiRequest.setServicedBy(null);
					break;
				}
			}
		}
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
				assert(mFreeTaxiesSet.contains(reportingTaxiAddress) == false) : 
					"Taxi " + msg.getFrom() + " should not already be in our free taxies list!";
				
				logger.info("handleTaxiStatusUpdate() adding to mFreeTaxiesList " + msg.getFrom());
				mFreeTaxiesSet.add(reportingTaxiAddress);
				break;
			case IN_REVISION:
			case BROKEN:
				if (mFreeTaxiesSet.contains(reportingTaxiAddress))
				{
					logger.info("handleTaxiStatusUpdate() removing from mFreeTaxiesList " + msg.getFrom());
					
					mFreeTaxiesSet.remove(reportingTaxiAddress);
				}
				else
				{
					logger.info("handleTaxiStatusUpdate() mTaxiesMap.get(reportingTaxiAddress) " + mTaxiesMap.get(reportingTaxiAddress));

					for(TaxiRequest taxiRequest : mTaxiRequests)
					{
						NetworkAddress taxiAddress = taxiRequest.getServicedBy();
						if((taxiAddress != null) && (taxiAddress.equals(reportingTaxiAddress)))
						{
							taxiRequest.setServicedBy(null);
							taxiRequest.setAsPendingProcessing();
							logger.info("handleTaxiStatusUpdate() clearing task of " + mTaxiesMap.get(taxiAddress));
							break;
						}
					}
				}
				//sendRevisionCompleteMessageTo(reportingTaxiAddress);
				break;
			default:
				assert(false);
			}
		}
	}
	
	private void sendRevisionCompleteMessageTo(NetworkAddress to)
	{
		/*
		logger.info("sendRevisionCompleteMessageTo() to " + to);
		
		RevisionCompleteMessage msg = new RevisionCompleteMessage(
				"Your revision is complete!", network.getAddress(), to);
		network.sendMessage(msg);
		withTaxi.sendRevisionCompleteMessage(msg);
		*/
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
					taxiRequest.setAsPendingProcessing();
					taxiRequest.setServicedBy(null);
					break;
				}
			}
		}
		// TODO this needs to be updated to account for 
		// all other possible reasons for which the taxi 
		// rejected our order
		//sendRevisionCompleteMessageTo(reportingTaxiAddress);
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
		List<TaxiRequest> pendingRequests = new LinkedList<TaxiRequest>();
		for (TaxiRequest taxiRequest : mTaxiRequests) 
		{
			switch(taxiRequest.getCurrentState())
			{
				case PENDING_PROCESSING:
					if((taxiRequest.hasTimedOut() == false) && 
							(taxiRequest.getRequestData().isValid()))
					{
						pendingRequests.add(taxiRequest);
					}
					else 
					{
						logger.info("processRequests() canceled request! ");
						
						taxiRequest.setAsCanceled();
					}
					break;
				case AWAITING_CONFIRMATION:
					if(taxiRequest.hasTimedOut() || 
							(taxiRequest.getRequestData().isValid() == false))
					{
						logger.info("processRequests() unconfirmed request! ");
						
						handleUnconfirmedRequest(taxiRequest);
					}
					break;
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
		if((pendingRequests.isEmpty() == false) && (mFreeTaxiesSet.isEmpty() == false))
		{
			Collections.sort(pendingRequests);
			confirmAndAssignRequests(pendingRequests);
		}
	}
	
	private void confirmAndAssignRequests(List<TaxiRequest> pendingRequests)
	{
		logger.info("confirmRequests() pendingRequests.size() " + pendingRequests.size());
		
		Iterator<NetworkAddress> iterator = mFreeTaxiesSet.iterator();
		while(iterator.hasNext())
		{
			if(pendingRequests.isEmpty())
			{
				break;
			}
			NetworkAddress taxiAddress = iterator.next();
			
			logger.info("confirmRequests() assigning to taxiAddress " + taxiAddress);
			
			UUID taxiID = mTaxiesMap.get(taxiAddress);
			Location taxiLocation = mLocationService.getAgentLocation(taxiID);
			
			TaxiRequest assignedRequest = getRequestNearestTo(pendingRequests, taxiLocation);
			pendingRequests.remove(assignedRequest);
			
			assignedRequest.setServicedBy(taxiAddress);
		
			if(assignedRequest.wasConfirmed())
			{
				assignedRequest.setAsReadyForService();
			}
			else
			{
				requestConfirmationFromUser(assignedRequest.getFrom(), 
					mLocationService.getAgentLocation(taxiID));
				assignedRequest.setAsAwaitingConfirmation();
			}
			iterator.remove();
		}
	}
	
	private TaxiRequest getRequestNearestTo(List<TaxiRequest> pendingRequests, Location taxiLocation)
	{
		logger.info("getRequestNearestTo() taxiLocation " + taxiLocation);
		
		TaxiRequest assignedRequest = null;
		double minDistance = Integer.MAX_VALUE;
		for(TaxiRequest taxiRequest: pendingRequests)
		{
			double distance = taxiRequest.getRequestData().getStartLocation().distanceTo(taxiLocation);
			if(minDistance > distance)
			{
				assignedRequest = taxiRequest;
				minDistance = distance;
			}
		}
		
		logger.info("getRequestNearestTo() request at " + assignedRequest.getRequestData().getStartLocation());
		
		return assignedRequest;
	}
	
	private void handleUnconfirmedRequest(TaxiRequest taxiRequest)
	{
		logger.info("handleUnconfirmedRequest() taxiRequest " + taxiRequest);
		
		assert(mFreeTaxiesSet.contains(taxiRequest.getServicedBy()) == false);
		
		if(taxiRequest.getServicedBy() != null)
		{
			mFreeTaxiesSet.add(taxiRequest.getServicedBy());
			taxiRequest.setServicedBy(null);
		}
		taxiRequest.setAsCanceled();
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
		
	private void requestConfirmationFromUser(NetworkAddress fromUser, Location taxiLocation)
	{
		logger.info("RequestConfirmationFromUser() fromUser " + fromUser);
		
		RequestTaxiConfirmationMessage message = new RequestTaxiConfirmationMessage(taxiLocation,
				network.getAddress(), fromUser);
		network.sendMessage(message);
	}
	
	private void sendReplyToUser(NetworkAddress toUser, UUID taxiID)
	{
		logger.info("SendReplyToUser() toUser " + toUser);
		
		TaxiServiceReply taxiServiceReply = new TaxiServiceReply(taxiID, "A taxi will be comming your way!");
		TaxiReplyMessage taxiServiceReplyMessage = 
				new TaxiReplyMessage(taxiServiceReply, network.getAddress(), toUser);
		network.sendMessage(taxiServiceReplyMessage);
	}
	
	private void sendOrderToTaxi(NetworkAddress toTaxi, ITransportServiceRequest request, NetworkAddress userNetworkAddress)
	{
		logger.info("SendOrderToTaxi() toTaxi " + toTaxi);
		
		UUID userID = request.getUserID();
		UUID authKey = request.getUserAuthKey();
		Location userLocation = request.getStartLocation();
		TaxiOrder taxiOrder = new TaxiOrder(userLocation, userID, authKey, userNetworkAddress);
		TaxiOrderMessage taxiOrderMessage = new TaxiOrderMessage(taxiOrder, network.getAddress(), toTaxi);
		//network.sendMessage(taxiOrderMessage);
		withTaxi.sendOrder(taxiOrderMessage);
	}
}
