package agents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import map.CityMap;

import agents.User.TransportPreference;

import conversations.busStationMediator.RegisterAsBusServiceMessage;
import conversations.taxiStationMediator.RegisterAsTaxiStationMessage;
import conversations.userBusStation.BusTravelPlanMessage;
import conversations.userMediator.messages.ConfirmTransportOfferMessage;
import conversations.userMediator.messages.TransportServiceOfferMessage;
import conversations.userMediator.messages.TransportServiceRequestMessage;
import conversations.userMediator.messages.messageData.ITransportServiceOffer;
import conversations.userMediator.messages.messageData.ITransportServiceRequest;
import conversations.userMediator.messages.messageData.TransportServiceOffer;
import conversations.userTaxi.messages.RequestTaxiConfirmationMessage;
import conversations.userTaxi.messages.TaxiRequestCancelMessage;
import conversations.userTaxi.messages.TaxiRequestConfirmationMessage;

import transportOffers.BusTransportOffer;
import transportOffers.TaxiTransportOffer;
import transportOffers.TransportOffer;
import transportOffers.WalkTransportOffer;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class Mediator extends AbstractParticipant
{
	private Set<NetworkAddress> mTaxiStations;
	private Set<NetworkAddress> mBusServices;
	private Queue<TransportServiceRequestMessage> mPendingServiceRequests;
	private Location mLocation = new Location(0, 0, 0);
	private CityMap mCityMap;
	
	private boolean mIsWalkingEnabled = true;
	private boolean mAreBusesEnabled = true;
	private boolean mAreTaxiesEnabled = true;
	
	private Map<NetworkAddress, ITransportServiceRequest> mUserTransportRequests;
	private Map<NetworkAddress, List<TransportOffer>> mUserTransportOffers;
	private Map<NetworkAddress, List<TransportOffer>> mOffersPendingConfirmation;
	
	private Comparator<TransportOffer> mSortOnTimeComparator;
	
	private class SortOnCostComparator implements Comparator<TransportOffer>
	{
		private double mTargetTime = 0.0;
		public SortOnCostComparator(double targetTime)
		{
			mTargetTime = targetTime;
		}
		@Override
		public int compare(TransportOffer o1, TransportOffer o2) 
		{
			if (o1 == o2)
			{
				return 0;
			}
			else 
			{
				double timeDiff1 = o1.getTravelTime() - mTargetTime;
				double timeDiff2 = o2.getTravelTime() - mTargetTime;
				
				if(timeDiff1 == timeDiff2)
				{
					double costDiff = o1.getCost() - o2.getCost();
					if(costDiff < 0)
					{
						return -1;
					}
					else if(costDiff > 0)
					{
						return 1;
					}
					return 0;
				}
				else if(timeDiff1 < 0 && timeDiff2 < 0)
				{
					if(timeDiff1 > timeDiff2)
					{
						return -1;
					}
					return 1;
				}
				else 
				{
					if(timeDiff1 < timeDiff2)
					{
						return -1;
					}
					return 1;
				}
			}
		}
	}
	
	public Mediator(UUID id, String name, CityMap cityMap) 
	{
		super(id, name);
		assert(cityMap != null);
		
		mCityMap = cityMap;
		mTaxiStations = new HashSet<NetworkAddress>();
		mBusServices = new HashSet<NetworkAddress>();
		mPendingServiceRequests = new ConcurrentLinkedQueue<TransportServiceRequestMessage>();
		mUserTransportRequests = new HashMap<NetworkAddress, ITransportServiceRequest>();
		mUserTransportOffers = new HashMap<NetworkAddress, List<TransportOffer>>();
		mOffersPendingConfirmation = new HashMap<NetworkAddress, List<TransportOffer>>();
		
		initTranportMethodComparators();
	}
	public void enableWalking(boolean enable)
	{
		mIsWalkingEnabled = enable;
	}
	public void enableTaxiUse(boolean enable)
	{
		mAreTaxiesEnabled = enable;
	}
	public void enableBusUse(boolean enable)
	{
		mAreBusesEnabled = enable;
	}
	
	private void initTranportMethodComparators()
	{
		mSortOnTimeComparator = new Comparator<TransportOffer>() 
		{
			@Override
			public int compare(TransportOffer o1, TransportOffer o2) 
			{
				if (o1 == o2)
				{
					return 0;
				}
				else 
				{
					double timeDiff = o1.getTravelTime() - o2.getTravelTime();
					if(timeDiff == 0)
					{
						double costDiff = o1.getCost() - o2.getCost();
						if(costDiff < 0)
						{
							return -1;
						}
						else if(costDiff > 0)
						{
							return 1;
						}
						return 0;
					}
					else if(timeDiff < 0)
					{
						return -1;
					}
					return 1;
				}
			}
		}; 
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() 
	{
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), mLocation));
		return ss;
	}
	
	@Override
	public void execute()
	{
		// pull in Messages from the network
		enqueueInput(this.network.getMessages());
		while (inputQueue.size() > 0) 
		{
			Input input = inputQueue.poll();
			if(input instanceof RequestTaxiConfirmationMessage)
			{
				RequestTaxiConfirmationMessage confirmationRequest = (RequestTaxiConfirmationMessage)input;
				NetworkAddress userAddress = confirmationRequest.getData().getUserAddress();
				if(mUserTransportOffers.containsKey(userAddress))
				{
					TaxiTransportOffer taxiOffer = new TaxiTransportOffer(confirmationRequest, network,
							userAddress);
					mUserTransportOffers.get(userAddress).add(taxiOffer);
				}
				else
				{
					cancelTaxiRequest(confirmationRequest);
				}
			}
			else if(input instanceof BusTravelPlanMessage)
			{
				BusTravelPlanMessage busTravelPlanMessage = (BusTravelPlanMessage)input;
				NetworkAddress userAddress = busTravelPlanMessage.getData().getUserAddress();
				if(mUserTransportOffers.containsKey(userAddress))
				{
					BusTransportOffer busOffer = new BusTransportOffer(busTravelPlanMessage);
					mUserTransportOffers.get(userAddress).add(busOffer);
				}
				// no else, because bus requests don't need to be canceled
			}
			else
			{
				processInput(input);
			}
		}
		if(mUserTransportOffers.isEmpty() == false)
		{
			sendTransportOffers();
		}
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input != null)
		{
			if(input instanceof TransportServiceRequestMessage)
			{
				processRequest((TransportServiceRequestMessage)input);
			}
			else if (input instanceof RegisterAsTaxiStationMessage)
			{
				processRequest((RegisterAsTaxiStationMessage)input);
			}
			else if (input instanceof RegisterAsBusServiceMessage)
			{
				processRequest((RegisterAsBusServiceMessage)input);
			}
			else if (input instanceof ConfirmTransportOfferMessage)
			{
				handleConfirmedTransportOffer((ConfirmTransportOfferMessage)input);
			}
		}
	}
	
	private void handleConfirmedTransportOffer(ConfirmTransportOfferMessage confirmedOfferMessage)
	{
		logger.info("handleConfirmedTransportOffer() " + confirmedOfferMessage);
		
		TransportOffer confirmedOffer = confirmedOfferMessage.getData();
		confirmedOffer.confirm();
		
		NetworkAddress userAddress = confirmedOfferMessage.getFrom();
		List<TransportOffer> transportOffers = mOffersPendingConfirmation.get(userAddress);
		for (Iterator<TransportOffer> iterator = transportOffers.iterator(); iterator.hasNext();) 
		{
			TransportOffer transportOffer = iterator.next();
			if(transportOffer != confirmedOffer)
			{
				transportOffer.cancel();
			}
		}
		mOffersPendingConfirmation.remove(userAddress);
	}
	
	private void cancelTaxiRequest(RequestTaxiConfirmationMessage requestToCancel)
	{
		logger.info("cancelTaxiRequest() " + requestToCancel);
		
		TaxiRequestCancelMessage cancelMessage = new TaxiRequestCancelMessage("I cancel this request", 
				network.getAddress(), requestToCancel.getFrom());
		network.sendMessage(cancelMessage);
	}
	
	public NetworkAddress getNetworkAddress() 
	{
		return network.getAddress();
	}
	
	@Override
	public void incrementTime()
	{
		super.incrementTime();
		
//		logger.info("incrementTime() " + getTime());
	}
	
	private void processRequest(TransportServiceRequestMessage serviceRequestMessage)
	{
		logger.info("processRequest() TransportServiceRequestMessage " + serviceRequestMessage);
		
		NetworkAddress userAddress = serviceRequestMessage.getFrom();
		mUserTransportRequests.put(userAddress, serviceRequestMessage.getData());
		mUserTransportOffers.put(userAddress, new ArrayList<TransportOffer>());
		
		mPendingServiceRequests.add(serviceRequestMessage);
		if(mTaxiStations.isEmpty() == false)
		{
			for (NetworkAddress toTaxiStation : mTaxiStations) 
			{
				forwardRequest(serviceRequestMessage, toTaxiStation);
			}
		}
		if(mBusServices.isEmpty() == false)
		{
			for (NetworkAddress toBusStation : mBusServices) 
			{
				forwardRequest(serviceRequestMessage, toBusStation);
			}
		}
	}
	
	private void processRequest(RegisterAsTaxiStationMessage registerMessage)
	{
		logger.info("processRequest() RegisterAsTaxiStationMessage " + registerMessage);
		
		NetworkAddress taxiStation = registerMessage.getFrom();
		mTaxiStations.add(taxiStation);
		if(mPendingServiceRequests.isEmpty() == false)
		{
			for (TransportServiceRequestMessage request : mPendingServiceRequests) 
			{
				if(request.getData().isValid())
				{
					forwardRequest(request, taxiStation);
				}
				else 
				{
					mPendingServiceRequests.remove(request);
				}
			}
		}
	}
	
	private void processRequest(RegisterAsBusServiceMessage registerMessage)
	{
		logger.info("processRequest() RegisterAsBusServiceMessage " + registerMessage);
		
		NetworkAddress busService = registerMessage.getFrom();
		mBusServices.add(busService);
		if(mPendingServiceRequests.isEmpty() == false)
		{
			for (TransportServiceRequestMessage request : mPendingServiceRequests) 
			{
				if(request.getData().isValid())
				{
					forwardRequest(request, busService);
				}
				else 
				{
					mPendingServiceRequests.remove(request);
				}
			}
		}
	}
	
	private void forwardRequest(TransportServiceRequestMessage requestMessage, NetworkAddress toTransportStation)
	{
		logger.info("forwardRequest() requestMessage " + requestMessage);
		
		TransportServiceRequestMessage forwardMessage = new TransportServiceRequestMessage
				(requestMessage.getData(), network.getAddress(), toTransportStation);
		network.sendMessage(forwardMessage);
	}
	
	private void sendTransportOffers()
	{
		Iterator<Map.Entry<NetworkAddress, List<TransportOffer>>> iterator = mUserTransportOffers.entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<NetworkAddress, List<TransportOffer>> entry = iterator.next();
			NetworkAddress userAddress = entry.getKey();
			List<TransportOffer> transportOffers = entry.getValue();
			if(canSendOffers(transportOffers, userAddress))
			{
				logger.info("sendTransportOffers() Sending offers!");
				
				addWalkingOffer(userAddress, transportOffers);
				
				ITransportServiceRequest request = mUserTransportRequests.get(userAddress);
				applyTransportPreference(request.getTransportPreference(), transportOffers);
				sortTransportOffers(request, transportOffers);
				sendOffersToUser(userAddress, transportOffers);
				
				iterator.remove();
				mOffersPendingConfirmation.put(userAddress, transportOffers);
			}
		}
	}
	private boolean canSendOffers(List<TransportOffer> transportOffers, NetworkAddress toUser)
	{
		boolean canSend = false;
		if(mAreBusesEnabled || mAreTaxiesEnabled)
		{
			if(transportOffers.size() > 0)
			{
				canSend = true;
			}
			else if(mIsWalkingEnabled)
			{
				ITransportServiceRequest request = mUserTransportRequests.get(toUser);
				int triggerTime = request.getTargetTravelTime() * 5;
				if(request.getRequestAge() > triggerTime)
				{
					canSend = true;
				}
			}
		}
		else if(mIsWalkingEnabled)
		{
			canSend = true;
		}
		return canSend;
	}
	private void addWalkingOffer(NetworkAddress userAddress, List<TransportOffer> transportOffers)
	{
		if(mIsWalkingEnabled)
		{
			ITransportServiceRequest request = mUserTransportRequests.get(userAddress);
			transportOffers.add(getWalkingTransportOffer(request));
		}
	}
	
	private WalkTransportOffer getWalkingTransportOffer(ITransportServiceRequest request)
	{
		List<Location> walkPath = mCityMap.getPath(request.getStartLocation(), request.getDestination());
		return new WalkTransportOffer(walkPath);
	}
	
	private void sortTransportOffers(ITransportServiceRequest request, List<TransportOffer> transportOffers)
	{
		logger.info("sortTransportOffers()");
				
		if(transportOffers.size() > 1)
		{
			switch (request.getTransportSortingPreference()) 
			{
			case PREFER_CHEAPEST:
				Collections.sort(transportOffers, new SortOnCostComparator(request.getTargetTravelTime()));
				break;
			case PREFER_FASTEST:
				Collections.sort(transportOffers, mSortOnTimeComparator);
				break;
			default:
				assert(false): "Sorting preference not handled " + request.getTransportSortingPreference();
				break;
			}
		}
	}
	
	private void applyTransportPreference(TransportPreference pref, List<TransportOffer> transportOffers)
	{
		double taxiCostScaling = pref.getTaxiCostScaling();
		double busCostScaling = pref.getBusCostScaling();
		double walkingCostScaling = pref.getWalkingCostScaling();
		
		for (Iterator<TransportOffer> iterator = transportOffers.iterator(); iterator.hasNext();) 
		{
			TransportOffer transportOffer = iterator.next();
			switch (transportOffer.getTransportMode()) 
			{
			case TAKE_TAXI:
				transportOffer.setCostScaleFactor(taxiCostScaling);
				break;
			case TAKE_BUS:
				transportOffer.setCostScaleFactor(busCostScaling);
				break;
			case WALKING:
				transportOffer.setCostScaleFactor(walkingCostScaling);
				break;
			default:
				assert(false) : "Transport mode not handled: " + transportOffer.getTransportMode();
				break;
			}
		}
	}
	
	private void sendOffersToUser(NetworkAddress userAddress, List<TransportOffer> transportOffers)
	{
		logger.info("sendOffersToUser() userAddress " + userAddress);
		
		ITransportServiceOffer offer = new TransportServiceOffer(transportOffers);
		TransportServiceOfferMessage msg = new TransportServiceOfferMessage(offer, network.getAddress(), 
				userAddress);
		network.sendMessage(msg);
	}
}
