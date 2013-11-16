package agents;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import messages.RegisterAsTaxiMessage;
import messages.RegisterAsTaxiStationMessage;
import messages.RejectOrderMessage;
import messages.RevisionCompleteMessage;
import messages.TaxiOrderCompleteMessage;
import messages.TaxiOrderMessage;
import messages.TaxiServiceReplyMessage;
import messages.RequestTaxiServiceConfirmationMessage;
import messages.TaxiServiceRequestConfirmationMessage;
import messages.TaxiServiceRequestMessage;
import messages.TaxiStatusUpdateMessage;


import messages.messageData.TaxiData;
import messages.messageData.TaxiOrder;
import messages.messageData.TaxiServiceReply;
import messages.messageData.taxiServiceRequest.TaxiServiceRequest;
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
		AWAITING_SERVICE,
		CONFIRMED,
		BEING_SERVICED,
		COMPLETED,
		CANCELED
	}
	
	private class TaxiRequest implements TimeDriven, Comparable<TaxiRequest>
	{
		private static final int DEFAULT_TIME_OUT = 1;
		
		private TaxiRequestState mCurrentState;
		private TaxiServiceRequestInterface mRequestData;
		private NetworkAddress mFromUserAddress;
		private NetworkAddress mServicedByTaxi;
		
		private int mCurrentTime;
		private int mTimeoutTime;
		
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
		}
		
		public NetworkAddress getServicedBy()
		{
			return mServicedByTaxi;
		}
		
		public void setAsAwaitingConfirmation()
		{
			assert(mCurrentState == TaxiRequestState.PENDING_PROCESSING);
			
			mCurrentState = TaxiRequestState.AWAITING_CONFIRMATION;
		}
		
		public void setAsConfirmed()
		{
			assert(mCurrentState == TaxiRequestState.AWAITING_CONFIRMATION);
			
			mCurrentState = TaxiRequestState.CONFIRMED;
		}
		
		public void setAsBeingServiced()
		{
			assert(mCurrentState != TaxiRequestState.COMPLETED);
			assert(mCurrentState != TaxiRequestState.CANCELED);
			
			mCurrentState = TaxiRequestState.BEING_SERVICED;
		}
		
		public void setAsCompleted()
		{
			assert(mCurrentState == TaxiRequestState.BEING_SERVICED);
			
			mCurrentState = TaxiRequestState.COMPLETED;
		}
		
		public void setAsCanceled()
		{
			assert(mCurrentState != TaxiRequestState.BEING_SERVICED);
			assert(mCurrentState != TaxiRequestState.COMPLETED);
			
			mCurrentState = TaxiRequestState.CANCELED;
		}
		
		public void setAsAwaitingService()
		{
			mCurrentState = TaxiRequestState.AWAITING_SERVICE;
		}

		@Override
		public void incrementTime() 
		{
			mCurrentTime++;
		}
		
		public boolean hasTimedOut()
		{
			return (mCurrentTime < mTimeoutTime);
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
				registerTaxi((RegisterAsTaxiMessage)input);
			}
			else if (input instanceof TaxiOrderCompleteMessage)
			{
				handleTaxiOrderComplete((TaxiOrderCompleteMessage)input);
			}
			else if (input instanceof TaxiStatusUpdateMessage)
			{
				handleTaxiStatusUpdate((TaxiStatusUpdateMessage)input);
			}
			else if(input instanceof RejectOrderMessage)
			{
				handleTaxiOrderRejected((RejectOrderMessage)input);
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
				taxiRequest.setAsConfirmed();
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
			assert(mFreeTaxiesList.contains(reportingTaxiAddress) == false);
			mFreeTaxiesList.add(reportingTaxiAddress);
			
			for(TaxiRequest taxiRequest : mTaxiRequests)
			{
				NetworkAddress taxiAddress = taxiRequest.getServicedBy();
				if((taxiAddress != null) && (taxiAddress.equals(reportingTaxiAddress)))
				{
					taxiRequest.setAsCompleted();
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
				assert(mFreeTaxiesList.contains(reportingTaxiAddress) == false);
				mFreeTaxiesList.add(reportingTaxiAddress);
				break;
			case IN_REVISION:
			case BROKEN:
				if (mFreeTaxiesList.contains(reportingTaxiAddress))
				{
					mFreeTaxiesList.remove(reportingTaxiAddress);
					sendRevisionCompleteMessageTo(reportingTaxiAddress);
				}
				else
				{
					logger.info("handleTaxiStatusUpdate() mTaxiesMap.get(reportingTaxiAddress) " + mTaxiesMap.get(reportingTaxiAddress));
				}
				break;
			default:
				assert(false);
			}
		}
	}
	
	private void sendRevisionCompleteMessageTo(NetworkAddress to)
	{
		logger.info("sendRevisionCompleteMessageTo() to " + to);
		
		RevisionCompleteMessage msg = new RevisionCompleteMessage(
				"Your revision is complete!", network.getAddress(), to);
		network.sendMessage(msg);
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
					taxiRequest.setAsAwaitingService();
					break;
				}
			}
		}
		// TODO this needs to be updated to account for 
		// all other possible reasons for which the taxi 
		// rejected our order
		sendRevisionCompleteMessageTo(reportingTaxiAddress);
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
		for (TaxiRequest taxiRequest : mTaxiRequests) 
		{
			taxiRequest.incrementTime();
		}
		if(mTaxiRequests.isEmpty() == false)
		{
			processRequests();
		}
	}
	
	private void processRequests()
	{
		Queue<TaxiRequest> pRequests = new PriorityBlockingQueue<TaxiRequest>();
		for (TaxiRequest taxiRequest : mTaxiRequests) 
		{
			switch(taxiRequest.getCurrentState())
			{
				case PENDING_PROCESSING:
				case AWAITING_SERVICE:
					if(taxiRequest.getRequestData().isValid())
					{
						pRequests.add(taxiRequest);
					}
					else
					{
						taxiRequest.setAsCanceled();
					}
					break;
				case AWAITING_CONFIRMATION:
					if(taxiRequest.hasTimedOut())
					{
						handleUnconfirmedRequest(taxiRequest);
					}
					break;
				case CONFIRMED:
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
			// TODO improve this
			if(assignedRequest.getCurrentState() == TaxiRequestState.PENDING_PROCESSING)
			{
				requestConfirmationFromUser(assignedRequest.getFrom(), 
					mLocationService.getAgentLocation(taxiID));
				assignedRequest.setAsAwaitingConfirmation();
			}
			else if (assignedRequest.getCurrentState() == TaxiRequestState.AWAITING_SERVICE)
			{
				if(assignedRequest.hasTimedOut())
				{
					assignedRequest.setAsCanceled();
				}
				else
				{
					assignedRequest.setAsConfirmed();
				}
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
		
		mFreeTaxiesList.add(taxiRequest.getServicedBy());
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
		network.sendMessage(taxiOrderMessage);
	}
}
