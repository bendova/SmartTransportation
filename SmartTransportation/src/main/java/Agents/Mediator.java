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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import map.CityMap;

import SmartTransportation.Simulation.TransportMethodCost;
import SmartTransportation.Simulation.TransportMethodSpeed;
import agents.User.TransportPreference;

import conversations.busStationMediator.RegisterAsBusServiceMessage;
import conversations.taxiStationMediator.RegisterAsTaxiStationMessage;
import conversations.taxiStationMediator.TaxiAvailableMessage;
import conversations.taxiStationMediator.messageData.ITaxiDescription;
import conversations.userBusStation.BusTravelPlanMessage;
import conversations.userMediator.messages.ConfirmTransportOfferMessage;
import conversations.userMediator.messages.TransportServiceOfferMessage;
import conversations.userMediator.messages.TransportServiceRequestMessage;
import conversations.userMediator.messages.messageData.ITransportServiceOffer;
import conversations.userMediator.messages.messageData.ITransportServiceRequest;
import conversations.userMediator.messages.messageData.TransportServiceOffer;

import transportOffers.BusTransportOffer;
import transportOffers.TaxiTransportOffer;
import transportOffers.TransportOffer;
import transportOffers.WalkTransportOffer;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class Mediator extends AbstractParticipant
{
	private final int RESERVED_TOP_OFFERS_COUNT = 1;
	
	private Set<NetworkAddress> mTaxiStations;
	private Set<NetworkAddress> mBusServices;
	private Queue<TransportServiceRequestMessage> mPendingServiceRequests;
	private ParticipantLocationService mLocationService;
	private Location mLocation = new Location(0, 0, 0);
	private CityMap mCityMap;
	private NetworkAddress mAddress;
	
	private boolean mIsWalkingEnabled = true;
	private boolean mAreBusesEnabled = true;
	private boolean mAreTaxiesEnabled = true;
	
	private Map<NetworkAddress, ITransportServiceRequest> mUserTransportRequests;
	private Map<NetworkAddress, List<TransportOffer>> mUserTransportOffers;
	private Map<NetworkAddress, List<TransportOffer>> mOffersPendingDelivery;	
	private Map<NetworkAddress, List<TransportOffer>> mOffersPendingConfirmation;
	
	private List<ITaxiDescription> mAvailableTaxies;
	
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
				int costDiff = (int) (o1.getCost() - o2.getCost());
				
				int timeDiff1 = (int)(o1.getTravelTime() - mTargetTime);
				int timeDiff2 = (int)(o2.getTravelTime() - mTargetTime);
				
				if(timeDiff1 == timeDiff2)
				{
					return (int) (o1.getCost() - o2.getCost());
				}
				else
				{
					int timeDiff = timeDiff1 - timeDiff2;
					return (costDiff * timeDiff);
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
		mUserTransportRequests = new ConcurrentHashMap<NetworkAddress, ITransportServiceRequest>();
		mUserTransportOffers = new ConcurrentHashMap<NetworkAddress, List<TransportOffer>>();
		mOffersPendingConfirmation = new HashMap<NetworkAddress, List<TransportOffer>>();
		mOffersPendingDelivery = new HashMap<NetworkAddress, List<TransportOffer>>();
		mAvailableTaxies = new ArrayList<ITaxiDescription>();
		
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
	
	public NetworkAddress getNetworkAddress() 
	{
		if(mAddress == null)
		{
			mAddress = network.getAddress();
		}
		return mAddress;
	}
	
	@Override
	public void initialise()
	{
		super.initialise();
		
		
		initializeLocationService();
//		initializeProtocols();
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
					int timeDiff = (int) (o1.getTravelTime() - o2.getTravelTime());
					if(timeDiff == 0)
					{
						return (int) (o1.getCost() - o2.getCost());
					}
					return timeDiff;
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
		enqueueInput(this.network.getMessages());
		while (inputQueue.size() > 0) 
		{
			Input input = inputQueue.poll();
			if(input instanceof TaxiAvailableMessage)
			{
				handleTaxiAvailable((TaxiAvailableMessage)input);
			}
			else if(input instanceof BusTravelPlanMessage)
			{
				handleBusPlan((BusTravelPlanMessage)input);
			}
			else
			{
				processInput(input);
			}
		}
		if(mUserTransportRequests.isEmpty() == false)
		{
			processTransportRequests();
		}
	}
	
	private void handleTaxiAvailable(TaxiAvailableMessage message)
	{
		if(mUserTransportRequests.isEmpty())
		{
			mAvailableTaxies.add(message.getData());
		}
		else
		{
			ITaxiDescription taxi = message.getData();
			NetworkAddress userAddress = getUserNearestTo(mUserTransportRequests.keySet(), 
					taxi.getTaxiLocation());
			mUserTransportOffers.get(userAddress).add(getTaxiTransportOffer(userAddress, taxi));
		}
	}
	
	private void handleBusPlan(BusTravelPlanMessage message)
	{
		NetworkAddress userAddress = message.getData().getUserAddress();
		if(mUserTransportOffers.containsKey(userAddress))
		{
			BusTransportOffer busOffer = new BusTransportOffer(message);
			mUserTransportOffers.get(userAddress).add(busOffer);
		}
		// TODO add the else case when the bus requests will need to be canceled
	}
	
	@Override
	protected void processInput(Input input) 
	{
		if(input instanceof TransportServiceRequestMessage)
		{
			handleTransportRequest((TransportServiceRequestMessage)input);
		}
		else if (input instanceof RegisterAsTaxiStationMessage)
		{
			registerTaxiStation((RegisterAsTaxiStationMessage)input);
		}
		else if (input instanceof RegisterAsBusServiceMessage)
		{
			registerBusService((RegisterAsBusServiceMessage)input);
		}
		else if (input instanceof ConfirmTransportOfferMessage)
		{
			handleConfirmedTransportOffer((ConfirmTransportOfferMessage)input);
		}
	}
	
	private void handleConfirmedTransportOffer(ConfirmTransportOfferMessage confirmedOfferMessage)
	{
		logger.info("handleConfirmedTransportOffer() from " + confirmedOfferMessage.getFrom());
		
		TransportOffer confirmedOffer = confirmedOfferMessage.getData();
		confirmedOffer.confirm();
		
		logger.info("handleConfirmedTransportOffer() confirmedOffer " + confirmedOffer);
		
		NetworkAddress userAddress = confirmedOfferMessage.getFrom();
		List<TransportOffer> transportOffers = mOffersPendingConfirmation.remove(userAddress);
		transportOffers.remove(confirmedOffer);
		for (Iterator<TransportOffer> iterator = transportOffers.iterator(); iterator.hasNext();) 
		{
			TransportOffer transportOffer = iterator.next();
			if(transportOffer instanceof TaxiTransportOffer)
			{
				mAvailableTaxies.add(((TaxiTransportOffer)transportOffer).getTaxiDescription());
			}
			else 
			{
				transportOffer.cancel();
			}
		}
	}
	
	private void handleTransportRequest(TransportServiceRequestMessage serviceRequestMessage)
	{
		logger.info("processRequest() TransportServiceRequestMessage " + serviceRequestMessage);
		
		NetworkAddress userAddress = serviceRequestMessage.getFrom();
		mUserTransportRequests.put(userAddress, serviceRequestMessage.getData());
		mUserTransportOffers.put(userAddress, new ArrayList<TransportOffer>());
		
		mPendingServiceRequests.add(serviceRequestMessage);
		if(mAvailableTaxies.isEmpty() == false)
		{
			ITaxiDescription taxi = getTaxiNearestTo(serviceRequestMessage.getData().getStartLocation());
			mUserTransportOffers.get(userAddress).add(getTaxiTransportOffer(userAddress, taxi));
		}
		if(mBusServices.isEmpty() == false)
		{
			for (NetworkAddress toBusStation : mBusServices) 
			{
				forwardRequest(serviceRequestMessage, toBusStation);
			}
		}
	}
	
	private TaxiTransportOffer getTaxiTransportOffer(NetworkAddress userAddress, 
			ITaxiDescription taxiDescription)
	{
		Location taxiLocation = taxiDescription.getTaxiLocation();
		
		ITransportServiceRequest request = mUserTransportRequests.get(userAddress);
		int travelToUserDistance = mCityMap.getPath(taxiLocation, request.getStartLocation()).size();
		int travelToDestinationDistance = mCityMap.getPath(request.getStartLocation(), 
				request.getDestination()).size();
		double totalTravelTime = (double)(travelToUserDistance + travelToDestinationDistance) 
				* TransportMethodSpeed.TAXI_SPEED.getTimeTakenPerUnitDistance();
		double travelCost = (double)travelToDestinationDistance 
				* TransportMethodCost.TAXI_COST.getCost();
		
		return new TaxiTransportOffer(travelCost, totalTravelTime, network, 
				taxiDescription, request);
	}
	
	private void registerTaxiStation(RegisterAsTaxiStationMessage registerMessage)
	{
		logger.info("processRequest() RegisterAsTaxiStationMessage " + registerMessage);
		
		mTaxiStations.add(registerMessage.getFrom());
	}
	
	private void registerBusService(RegisterAsBusServiceMessage registerMessage)
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
				(requestMessage.getData(), mAddress, toTransportStation);
		network.sendMessage(forwardMessage);
	}
	
	private void processTransportRequests()
	{
		if(mIsWalkingEnabled)
		{
			addWalkingOffers();
		}
		if(mUserTransportOffers.isEmpty() == false)
		{
			Iterator<Map.Entry<NetworkAddress, List<TransportOffer>>> iterator = mUserTransportOffers.
					entrySet().iterator();
			while(iterator.hasNext())
			{
				Map.Entry<NetworkAddress, List<TransportOffer>> entry = iterator.next();
				List<TransportOffer> transportOffers = entry.getValue();
				if(transportOffers.isEmpty() == false)
				{
					NetworkAddress userAddress = entry.getKey();
					sortTransportOffers(userAddress, transportOffers);
					reassignSurplusOffers(userAddress, transportOffers);
					
					mOffersPendingDelivery.put(userAddress, transportOffers);
				}
			}
			
			if(mOffersPendingDelivery.isEmpty() == false)
			{
				sendOffers();
			}
		}
	}
	
	private void addWalkingOffers()
	{
		Iterator<Map.Entry<NetworkAddress, ITransportServiceRequest>> iterator = mUserTransportRequests.
				entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<NetworkAddress, ITransportServiceRequest> entry = iterator.next();
			NetworkAddress userAddress = entry.getKey();
			ITransportServiceRequest request = entry.getValue();
			List<TransportOffer> transportOffers = mUserTransportOffers.get(userAddress);
			
			if(canAddWalkOffer(transportOffers, request))
			{
				transportOffers.add(getWalkingTransportOffer(request));
			}
		}	
	}
	
	private boolean canAddWalkOffer(List<TransportOffer> transportOffers, ITransportServiceRequest request)
	{
		boolean canAdd = false;
		if(mAreBusesEnabled || mAreTaxiesEnabled)
		{
			if(transportOffers.size() > 0)
			{
				canAdd = true;
			}
			else
			{
				int triggerTime = request.getTargetTravelTime();
				if(request.getRequestAge() > triggerTime)
				{
					canAdd = true;
				}
			}
		}
		else
		{
			canAdd = true;
		}
		return canAdd;
	}
	
	private WalkTransportOffer getWalkingTransportOffer(ITransportServiceRequest request)
	{
		List<Location> walkPath = mCityMap.getPath(request.getStartLocation(), request.getDestination());
		return new WalkTransportOffer(walkPath);
	}
	
	private void sortTransportOffers(NetworkAddress userAddress, List<TransportOffer> transportOffers)
	{
		ITransportServiceRequest request = mUserTransportRequests.get(userAddress);
		applyTransportPreference(request.getTransportPreference(), transportOffers);
		
		if(transportOffers.size() > 1)
		{
			Collections.sort(transportOffers, getComparatorFor(request));
		}
	}
	
	private void applyTransportPreference(TransportPreference pref, List<TransportOffer> transportOffers)
	{
		for (Iterator<TransportOffer> iterator = transportOffers.iterator(); iterator.hasNext();) 
		{
			applyTransportPreference(pref, iterator.next());
		}
	}
	
	private void applyTransportPreference(TransportPreference pref, TransportOffer transportOffer)
	{
		double taxiCostScaling = pref.getTaxiCostScaling();
		double busCostScaling = pref.getBusCostScaling();
		double walkingCostScaling = pref.getWalkingCostScaling();
		
		switch (transportOffer.getTransportMode()) 
		{
		case TAKE_TAXI:
			transportOffer.scaleCost(taxiCostScaling);
			break;
		case TAKE_BUS:
			transportOffer.scaleCost(busCostScaling);
			break;
		case WALKING:
			transportOffer.scaleCost(walkingCostScaling);
			break;
		default:
			assert(false) : "Transport mode not handled: " + transportOffer.getTransportMode();
			break;
		}
	}
	
	private Comparator<TransportOffer> getComparatorFor(ITransportServiceRequest request)
	{
		switch (request.getTransportSortingPreference()) 
		{
		case PREFER_CHEAPEST:
			return new SortOnCostComparator(request.getTargetTravelTime());
		case PREFER_FASTEST:
			return mSortOnTimeComparator;
		default:
			assert(false): "Sorting preference not handled " + request.getTransportSortingPreference();
			return null;
		}
	}
	
	private void reassignSurplusOffers(NetworkAddress userAddress, List<TransportOffer> offers)
	{
		if(mUserTransportRequests.size() < 2)
		{
			return;
		}
		
		boolean wasOfferShared = false;
		
		int offerIndex = RESERVED_TOP_OFFERS_COUNT;
		int offersCount = offers.size();
		while((offerIndex < offersCount) && (offersCount > RESERVED_TOP_OFFERS_COUNT))
		{
			TransportOffer offer = offers.get(offerIndex);
			if(offer instanceof TaxiTransportOffer)
			{
				wasOfferShared = shareOffer(userAddress, (TaxiTransportOffer)offer);
			}
			
			if(wasOfferShared)
			{
				--offersCount;
			}
			else 
			{
				++offerIndex;
			}
		}
	}
	
	private boolean shareOffer(NetworkAddress sharingUser, TaxiTransportOffer offerToShare)
	{
		int maxCostGain = 0;
		NetworkAddress selectedUser = null;
		
		Iterator<Map.Entry<NetworkAddress, List<TransportOffer>>> iterator = mUserTransportOffers.
				entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<NetworkAddress, List<TransportOffer>> entry = iterator.next();
			NetworkAddress user = entry.getKey(); 
			if(user.equals(sharingUser) == false)
			{
				ITransportServiceRequest request = mUserTransportRequests.get(user);
				TransportPreference pref = request.getTransportPreference();
				applyTransportPreference(pref, offerToShare);
				
				int costGain;
				List<TransportOffer> currentOffers = entry.getValue();
				if(currentOffers.isEmpty())
				{
					costGain = (int)offerToShare.getCost();
				}
				else
				{
					TransportOffer currentTopOffer = entry.getValue().get(0);
					Comparator<TransportOffer> comparator = getComparatorFor(request);
					costGain = comparator.compare(currentTopOffer, offerToShare);
				}
				
				if(costGain > maxCostGain)
				{
					maxCostGain = costGain;
					selectedUser = user;
				}
			}
		}
		
		if(selectedUser != null)
		{
			mUserTransportOffers.get(sharingUser).remove(offerToShare);
			
			offerToShare.setTransportServiceRequest(mUserTransportRequests.get(selectedUser));
			mUserTransportOffers.get(selectedUser).add(0, offerToShare);
			
			return true;
		}
		else
		{
			ITransportServiceRequest request = mUserTransportRequests.get(sharingUser);
			TransportPreference pref = request.getTransportPreference();
			applyTransportPreference(pref, offerToShare);
			
			return false;
		}
	}
	
	private void sendOffers()
	{
		logger.info("sendOffers()");
		
		Iterator<Map.Entry<NetworkAddress, List<TransportOffer>>> iterator = mOffersPendingDelivery.
				entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<NetworkAddress, List<TransportOffer>> entry = iterator.next();
			List<TransportOffer> transportOffers = entry.getValue();
			NetworkAddress userAddress = entry.getKey();
			
			sendOffersToUser(userAddress, transportOffers);
			
			mUserTransportOffers.remove(userAddress);
			mUserTransportRequests.remove(userAddress);
			mOffersPendingConfirmation.put(userAddress, transportOffers);
			iterator.remove();
		}
	}
	
	private void sendOffersToUser(NetworkAddress userAddress, List<TransportOffer> transportOffers)
	{
		logger.info("sendOffersToUser() userAddress " + userAddress);
		
		ITransportServiceOffer offer = new TransportServiceOffer(transportOffers);
		TransportServiceOfferMessage msg = new TransportServiceOfferMessage(offer, mAddress, 
				userAddress);
		network.sendMessage(msg);
	}
	
	private NetworkAddress getUserNearestTo(Set<NetworkAddress> userAddresses, Location location)
	{
		NetworkAddress nearestUser = null;
		double minDistance = Integer.MAX_VALUE;
		for(NetworkAddress userAddress: userAddresses)
		{
			double distance = mUserTransportRequests.get(userAddress).getStartLocation().distanceTo(location);
			if(minDistance > distance)
			{
				nearestUser = userAddress;
				minDistance = distance;
			}
		}
		return nearestUser;
	}
	
	private ITaxiDescription getTaxiNearestTo(Location location)
	{
		ITaxiDescription nearestTaxi = null;
		double minDistance = Integer.MAX_VALUE;
		for(ITaxiDescription taxi: mAvailableTaxies)
		{
			double distance = taxi.getTaxiLocation().distanceTo(location);
			if(minDistance > distance)
			{
				nearestTaxi = taxi;
				minDistance = distance;
			}
		}
		return nearestTaxi;
	}
}
