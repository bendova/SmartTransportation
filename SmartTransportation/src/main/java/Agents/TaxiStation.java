package agents;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import javax.sound.midi.VoiceStatus;

import org.antlr.grammar.v3.ANTLRv3Parser.finallyClause_return;

import messageData.TaxiData;
import messageData.TaxiOrder;
import messageData.TaxiServiceReply;
import messageData.taxiServiceRequest.TaxiServiceRequest;
import messageData.taxiServiceRequest.TaxiServiceRequestInterface;
import messages.RegisterAsTaxiMessage;
import messages.RegisterAsTaxiStationMessage;
import messages.TaxiOrderCompleteMessage;
import messages.TaxiOrderMessage;
import messages.TaxiServiceReplyMessage;
import messages.RequestTaxiServiceConfirmationMessage;
import messages.TaxiServiceRequestConfirmationMessage;
import messages.TaxiServiceRequestMessage;


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
		CONFIRMED,
		BEING_SERVICED,
		COMPLETED,
		CANCELED
	}
	
	private class TaxiRequest implements TimeDriven, Comparable<TaxiRequest>
	{
		private static final int DEFAULT_TIME_OUT = 2;
		
		private TaxiRequestState mCurrentState;
		private TaxiServiceRequestInterface mRequestData;
		private NetworkAddress mFromUserAddress;
		private UUID mServicedByTaxiID;
		
		private int mCurrentTime;
		private int mTimeoutTime;
		
		public TaxiRequest(TaxiServiceRequestMessage requestMessage, int currentTime)
		{
			this(requestMessage, currentTime, DEFAULT_TIME_OUT);
		}
		
		public TaxiRequest(TaxiServiceRequestMessage requestMessage, int currentTime, int timeOutTimeSteps)
		{
			assert(requestMessage != null);
			assert(currentTime >= 0);
			
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
		
		public void setServicedBy(UUID taxiID)
		{
			mServicedByTaxiID = taxiID;
		}
		
		public UUID getServicedBy()
		{
			return mServicedByTaxiID;
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
	private Map<UUID, NetworkAddress> mTaxiesMap;
	private List<UUID> mFreeTaxiesList;
	private ParticipantLocationService mLocationService;
	
	public TaxiStation(UUID id, String name, Location location, NetworkAddress mediatorNetworkAddress) 
	{
		super(id, name);
		mLocation = location;
		mMediatorAddress = mediatorNetworkAddress;
		
		mTaxiesMap = new HashMap<UUID, NetworkAddress>();
		mFreeTaxiesList = new LinkedList<UUID>();
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
		}
	}
	
	private void storeRequest(TaxiServiceRequestMessage taxiRequestMessage)
	{
		logger.info("storeRequest()");
		
		mTaxiRequests.add(new TaxiRequest(taxiRequestMessage, getTime().intValue()));
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
			mTaxiesMap.put(newTaxiData.getID(), newTaxiData.getNetworkAddress());
			mFreeTaxiesList.add(newTaxiData.getID());
		}
	}
	
	private void handleTaxiOrderComplete(TaxiOrderCompleteMessage taxiOrderCompleteMessage)
	{
		logger.info("handleTaxiOrderComplete() taxiOrderCompleteMessage " + taxiOrderCompleteMessage);
		
		UUID reportingTaxi = taxiOrderCompleteMessage.getData();
		if(mTaxiesMap.containsKey(reportingTaxi))
		{
			mFreeTaxiesList.add(reportingTaxi);
			
			for(TaxiRequest taxiRequest : mTaxiRequests)
			{
				UUID taxiID = taxiRequest.getServicedBy();
				if((taxiID != null) && (taxiID.equals(reportingTaxi)))
				{
					taxiRequest.setAsCompleted();
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
		if(mTaxiRequests.isEmpty() == false)
		{
			processRequests();
		}
	}
	
	private void processRequests()
	{
		for (TaxiRequest taxiRequest : mTaxiRequests) 
		{
			switch(taxiRequest.getCurrentState())
			{
				case PENDING_PROCESSING:
					if(mFreeTaxiesList.isEmpty() == false)
					{
						confirmRequest(taxiRequest);
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
	}
	
	private void confirmRequest(TaxiRequest taxiRequest)
	{
		logger.info("confirmRequest() taxiRequest " + taxiRequest);
		
		TaxiServiceRequestInterface taxiServiceRequest = taxiRequest.getRequestData();
		if((taxiServiceRequest != null) && (taxiServiceRequest.isValid()))
		{
			logger.info("processRequest() Processing request: " + taxiServiceRequest.getMessage());
			
			Location userLocation = taxiServiceRequest.getLocation();
			UUID nearestTaxiID = findFreeTaxiNearestTo(userLocation);
			if(nearestTaxiID != null)
			{
				requestConfirmationFromUser(taxiRequest.getFrom(), mLocationService.getAgentLocation(nearestTaxiID));
				mFreeTaxiesList.remove(nearestTaxiID);
				taxiRequest.setServicedBy(nearestTaxiID);
				taxiRequest.setAsAwaitingConfirmation();
			}
		}
		else
		{
			taxiRequest.setAsCanceled();
		}
	}
	
	private void handleUnconfirmedRequest(TaxiRequest taxiRequest)
	{
		logger.info("handleUnconfirmedRequest() taxiRequest " + taxiRequest);
		
		mFreeTaxiesList.add(taxiRequest.getServicedBy());
		taxiRequest.setAsCanceled();
	}
	
	private void handleConfirmedRequest(TaxiRequest taxiRequest)
	{
		logger.info("handleConfirmedRequest() taxiRequest " + taxiRequest);
		
		UUID taxiID = taxiRequest.getServicedBy();
		NetworkAddress taxiAddress = mTaxiesMap.get(taxiID);
		sendReplyToUser(taxiRequest.getFrom(), taxiID);
		
		TaxiServiceRequestInterface requestData = taxiRequest.getRequestData();
		sendOrderToTaxi(taxiAddress, requestData.getLocation(), requestData.getUserID(),
				taxiRequest.getFrom());
		taxiRequest.setAsBeingServiced();
	}
	
	private UUID findFreeTaxiNearestTo(Location userLocation)
	{
		logger.info("FindFreeTaxiNearestTo() userLocation " + userLocation);
		
		UUID nearestTaxiID = null;
		Location nearestTaxiLocation = null;
		if(mFreeTaxiesList.isEmpty() == false)
		{
			for (UUID freeTaxiID : mFreeTaxiesList) 
			{
				Location taxiLocation = mLocationService.getAgentLocation(freeTaxiID);
				if((nearestTaxiLocation == null) || 
					(taxiLocation.distanceTo(userLocation) < nearestTaxiLocation.distanceTo(userLocation)))
				{
					nearestTaxiLocation = taxiLocation;
					nearestTaxiID = freeTaxiID;
				}
			}
			logger.info("FindFreeTaxiNearestTo() nearestTaxiLocation " + nearestTaxiLocation);
		}
		return nearestTaxiID;
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
	
	private void sendOrderToTaxi(NetworkAddress toTaxi, Location userLocation, UUID userID, NetworkAddress userNetworkAddress)
	{
		logger.info("SendOrderToTaxi() toTaxi " + toTaxi);
		
		TaxiOrder taxiOrder = new TaxiOrder(userLocation, userID, userNetworkAddress);
		TaxiOrderMessage taxiOrderMessage = new TaxiOrderMessage(taxiOrder, network.getAddress(), toTaxi);
		network.sendMessage(taxiOrderMessage);
	}
}
