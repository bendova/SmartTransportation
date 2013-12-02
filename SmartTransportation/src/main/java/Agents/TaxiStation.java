package agents;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import conversations.protocols.taxistation.ProtocolWithTaxi;
import conversations.taxistationtaxi.actions.OnOrderCompleteAction;
import conversations.taxistationtaxi.actions.OnRegisterAction;
import conversations.taxistationtaxi.actions.OnRejectOrderAction;
import conversations.taxistationtaxi.actions.OnTaxiStatusUpdateAction;
import messages.*;
import messages.messageData.*;
import messages.messageData.taxiServiceRequest.TaxiServiceRequestInterface;
import uk.ac.imperial.presage2.core.TimeDriven;
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
		private TaxiServiceRequestInterface mRequestData;
		private NetworkAddress mFromUserAddress;
		private NetworkAddress mServicedByTaxi;
		
		private int mCurrentTime;
		private int mTimeoutTime;
		
		private boolean mWasConfirmed;
		private boolean mIsAssigned;
		
		public TaxiRequest(TaxiServiceRequestMessage requestMessage)
		{
			this(requestMessage, 0, DEFAULT_TIME_OUT);
		}
		
		public TaxiRequest(TaxiServiceRequestMessage requestMessage, int currentTime, int timeOutTimeSteps)
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
		
		public TaxiServiceRequestInterface getRequestData()
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
	private List<NetworkAddress> mFreeTaxiesList;
	private ParticipantLocationService mLocationService;
	private ProtocolWithTaxi withTaxi;
	
	public TaxiStation(UUID id, String name, Location location, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mMediatorAddress = mediatorNetworkAddress;
		
		mTaxiesMap = new HashMap<NetworkAddress, UUID>();
		mFreeTaxiesList = new LinkedList<NetworkAddress>();
		mTaxiRequests = new PriorityBlockingQueue<TaxiRequest>();
	}
	
	public NetworkAddress getNetworkAddress() 
	{
		return network.getAddress();
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
			if(input instanceof TaxiServiceRequestMessage)
			{
				storeRequest((TaxiServiceRequestMessage)input);
			}
			else if (input instanceof TaxiServiceRequestConfirmationMessage)
			{
				processRequestConfirmation((TaxiServiceRequestConfirmationMessage)input);
			}
			else if (input instanceof RegisterAsTaxiMessage)
			{
				//registerTaxi((RegisterAsTaxiMessage)input);
				withTaxi.handleRegisterTaxiMessage((RegisterAsTaxiMessage)input);
			}
			else if (input instanceof TaxiOrderCompleteMessage)
			{
				//handleTaxiOrderComplete((TaxiOrderCompleteMessage)input);
				withTaxi.handleOrderComplete((TaxiOrderCompleteMessage)input);
			}
			else if (input instanceof TaxiStatusUpdateMessage)
			{
				//handleTaxiStatusUpdate((TaxiStatusUpdateMessage)input);
				withTaxi.handleTaxiStatusUpdate((TaxiStatusUpdateMessage)input);
			}
			else if(input instanceof RejectOrderMessage)
			{
				//handleTaxiOrderRejected((RejectOrderMessage)input);
				withTaxi.handleOrderReject((RejectOrderMessage)input);
			}
		}
	}
	
	private void storeRequest(TaxiServiceRequestMessage taxiRequestMessage)
	{
		logger.info("storeRequest()");
		
		mTaxiRequests.add(new TaxiRequest(taxiRequestMessage));
	}
	
	private void processRequestConfirmation(TaxiServiceRequestConfirmationMessage requestConfirmationMessage)
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
			mFreeTaxiesList.add(taxiAddress);
		}
	}
	
	private void handleTaxiOrderComplete(TaxiOrderCompleteMessage taxiOrderCompleteMessage)
	{
		logger.info("handleTaxiOrderComplete() taxiOrderCompleteMessage " + taxiOrderCompleteMessage);
		
		NetworkAddress reportingTaxiAddress = taxiOrderCompleteMessage.getFrom();
		if(mTaxiesMap.containsKey(reportingTaxiAddress))
		{
			if(mFreeTaxiesList.contains(reportingTaxiAddress))
			{
				logger.info("handleTaxiOrderComplete() Logic error!");
			}
			
			assert(mFreeTaxiesList.contains(reportingTaxiAddress) == false);
			mFreeTaxiesList.add(reportingTaxiAddress);
			
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
				assert(mFreeTaxiesList.contains(reportingTaxiAddress) == false) : 
					"Taxi " + msg.getFrom() + " should not already be in our free taxies list!";
				
				logger.info("handleTaxiStatusUpdate() adding to mFreeTaxiesList " + msg.getFrom());
				mFreeTaxiesList.add(reportingTaxiAddress);
				break;
			case IN_REVISION:
			case BROKEN:
				if (mFreeTaxiesList.contains(reportingTaxiAddress))
				{
					logger.info("handleTaxiStatusUpdate() removing from mFreeTaxiesList " + msg.getFrom());
					
					mFreeTaxiesList.remove(reportingTaxiAddress);
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
		Queue<TaxiRequest> pRequests = new PriorityBlockingQueue<TaxiRequest>();
		
		logger.info("processRequests() mTaxiRequests.size() " + mTaxiRequests.size());
		
		for (TaxiRequest taxiRequest : mTaxiRequests) 
		{
			switch(taxiRequest.getCurrentState())
			{
				case PENDING_PROCESSING:
					if((taxiRequest.hasTimedOut() == false) && 
							(taxiRequest.getRequestData().isValid()))
					{
						pRequests.add(taxiRequest);
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
		if((pRequests.isEmpty() == false) && (mFreeTaxiesList.isEmpty() == false))
		{
			confirmAndAssignRequests(pRequests);
		}
	}
	
	private void confirmAndAssignRequests(Queue<TaxiRequest> pendingRequests)
	{
		logger.info("confirmRequests() pendingRequests.size() " + pendingRequests.size());
		
		Iterator<NetworkAddress> iterator = mFreeTaxiesList.iterator();
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
	
	private TaxiRequest getRequestNearestTo(Queue<TaxiRequest> pendingRequests, Location taxiLocation)
	{
		logger.info("getRequestNearestTo() taxiLocation " + taxiLocation);
		
		TaxiRequest assignedRequest = pendingRequests.peek();
		
		assert(assignedRequest != null);
		double minDistance = assignedRequest.getRequestData().getLocation().distanceTo(taxiLocation);
		for(TaxiRequest taxiRequest: pendingRequests)
		{
			double distance = taxiRequest.getRequestData().getLocation().distanceTo(taxiLocation);
			if(minDistance > distance)
			{
				assignedRequest = taxiRequest;
				minDistance = distance;
			}
		}
		
		logger.info("getRequestNearestTo() request at " + assignedRequest.getRequestData().getLocation());
		
		return assignedRequest;
	}
	
	private void handleUnconfirmedRequest(TaxiRequest taxiRequest)
	{
		logger.info("handleUnconfirmedRequest() taxiRequest " + taxiRequest);
		
		assert(mFreeTaxiesList.contains(taxiRequest.getServicedBy()) == false);
		
		if(taxiRequest.getServicedBy() != null)
		{
			mFreeTaxiesList.add(taxiRequest.getServicedBy());
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
		
		RequestTaxiServiceConfirmationMessage message = new RequestTaxiServiceConfirmationMessage(taxiLocation,
				network.getAddress(), fromUser);
		network.sendMessage(message);
	}
	
	private void sendReplyToUser(NetworkAddress toUser, UUID taxiID)
	{
		logger.info("SendReplyToUser() toUser " + toUser);
		
		TaxiServiceReply taxiServiceReply = new TaxiServiceReply(taxiID, "A taxi will be comming your way!");
		TaxiServiceReplyMessage taxiServiceReplyMessage = 
				new TaxiServiceReplyMessage(taxiServiceReply, network.getAddress(), toUser);
		network.sendMessage(taxiServiceReplyMessage);
	}
	
	private void sendOrderToTaxi(NetworkAddress toTaxi, TaxiServiceRequestInterface request, NetworkAddress userNetworkAddress)
	{
		logger.info("SendOrderToTaxi() toTaxi " + toTaxi);
		
		UUID userID = request.getUserID();
		UUID authKey = request.getUserAuthKey();
		Location userLocation = request.getLocation();
		TaxiOrder taxiOrder = new TaxiOrder(userLocation, userID, authKey, userNetworkAddress);
		TaxiOrderMessage taxiOrderMessage = new TaxiOrderMessage(taxiOrder, network.getAddress(), toTaxi);
		//network.sendMessage(taxiOrderMessage);
		withTaxi.sendOrder(taxiOrderMessage);
	}
}
