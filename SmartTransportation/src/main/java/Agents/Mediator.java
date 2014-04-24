package agents;
import java.util.ArrayList;
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

import SmartTransportation.Simulation.TransportMethodCost;
import SmartTransportation.Simulation.TransportMethodSpeed;
import agents.User.TransportPreference;
import agents.helpers.ITransportServiceRecord;
import agents.helpers.TransportServiceRecord;

import conversations.busStationMediator.RegisterAsBusServiceMessage;
import conversations.taxiStationMediator.RegisterAsTaxiStationMessage;
import conversations.taxiStationMediator.TaxiAvailableMessage;
import conversations.taxiStationMediator.messageData.ITaxiDescription;
import conversations.userBusStation.BusTravelPlanMessage;
import conversations.userMediator.messages.ConfirmTransportOfferMessage;
import conversations.userMediator.messages.TransportServiceRequestMessage;
import conversations.userMediator.messages.messageData.ITransportServiceRequest;

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
	private NetworkAddress mNetworkAddress;
	
	private boolean mIsWalkingEnabled = true;
	private boolean mAreBusesEnabled = true;
	private boolean mAreTaxiesEnabled = true;
	
	private Map<NetworkAddress, ITransportServiceRecord> mTransportServiceRecords;
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
		
		mTransportServiceRecords = new HashMap<NetworkAddress, ITransportServiceRecord>();
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
		if(mNetworkAddress == null)
		{
			mNetworkAddress = network.getAddress();
		}
		return mNetworkAddress;
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
		if(mTransportServiceRecords.isEmpty() == false)
		{
			processTransportServiceRecords();
		}
	}
	
	private void handleTaxiAvailable(TaxiAvailableMessage message)
	{
		if(mTransportServiceRecords.isEmpty())
		{
			mAvailableTaxies.add(message.getData());
		}
		else
		{
			ITaxiDescription taxi = message.getData();
			NetworkAddress userAddress = getUserNearestTo(mTransportServiceRecords.keySet(), 
					taxi.getTaxiLocation());
			mTransportServiceRecords.get(userAddress).addTransportOffer(getTaxiTransportOffer(userAddress, taxi));
		}
	}
	
	private void handleBusPlan(BusTravelPlanMessage message)
	{
		NetworkAddress userAddress = message.getData().getUserAddress();
		if(mTransportServiceRecords.containsKey(userAddress))
		{
			BusTransportOffer busOffer = new BusTransportOffer(message);
			mTransportServiceRecords.get(userAddress).addTransportOffer(busOffer);
		}
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
		
		ITransportServiceRecord transportServiceRecord = mTransportServiceRecords.remove(userAddress);
		List<TransportOffer> transportOffers = transportServiceRecord.getTransportOffers();
		transportServiceRecord.removeTransportOffer(confirmedOffer);
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
		ITransportServiceRequest serviceRequest = serviceRequestMessage.getData();
		ITransportServiceRecord serviceRecord = new TransportServiceRecord(userAddress, 
				serviceRequest, mNetworkAddress, network, getComparatorFor(serviceRequest));
		mTransportServiceRecords.put(userAddress, serviceRecord);
		
		mPendingServiceRequests.add(serviceRequestMessage);
		if(mAvailableTaxies.isEmpty() == false)
		{
			ITaxiDescription taxi = getTaxiNearestTo(serviceRequestMessage.getData().getStartLocation());
			serviceRecord.addTransportOffer(getTaxiTransportOffer(userAddress, taxi));
		}
		if(mBusServices.isEmpty() == false)
		{
			for (NetworkAddress toBusStation : mBusServices) 
			{
				forwardRequest(serviceRequestMessage, toBusStation);
			}
		}
		if(mIsWalkingEnabled)
		{
			serviceRecord.addTransportOffer(getWalkingTransportOffer(serviceRequest));
		}
	}
	
	private TaxiTransportOffer getTaxiTransportOffer(NetworkAddress userAddress, 
			ITaxiDescription taxiDescription)
	{
		Location taxiLocation = taxiDescription.getTaxiLocation();
		
		ITransportServiceRequest request = mTransportServiceRecords.get(userAddress).getTransportServiceRequest();
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
				(requestMessage.getData(), mNetworkAddress, toTransportStation);
		network.sendMessage(forwardMessage);
	}
	
	private void processTransportServiceRecords()
	{
		if(mTransportServiceRecords.isEmpty() == false)
		{
			Iterator<Map.Entry<NetworkAddress, ITransportServiceRecord>> iterator = mTransportServiceRecords.
					entrySet().iterator();
			while(iterator.hasNext())
			{
				Map.Entry<NetworkAddress, ITransportServiceRecord> entry = iterator.next();
				List<TransportOffer> transportOffers = entry.getValue().getTransportOffers();
				if(transportOffers.isEmpty() == false)
				{
					entry.getValue().sortTransportOffers();
					NetworkAddress userAddress = entry.getKey();
					reassignSurplusOffers(userAddress, transportOffers);
				}
			}
			sendOffers();
		}
	}
	
	private WalkTransportOffer getWalkingTransportOffer(ITransportServiceRequest request)
	{
		List<Location> walkPath = mCityMap.getPath(request.getStartLocation(), request.getDestination());
		return new WalkTransportOffer(walkPath);
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
		if(mTransportServiceRecords.size() < 2)
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
		
		Iterator<Map.Entry<NetworkAddress, ITransportServiceRecord>> iterator = mTransportServiceRecords.
				entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<NetworkAddress, ITransportServiceRecord> entry = iterator.next();
			NetworkAddress user = entry.getKey(); 
			if(user.equals(sharingUser) == false)
			{
				ITransportServiceRecord serviceRecord = mTransportServiceRecords.get(user);
				ITransportServiceRequest request = serviceRecord.getTransportServiceRequest();
				TransportPreference pref = request.getTransportPreference();
				offerToShare.applyTransportPreference(pref);
				
				int costGain;
				List<TransportOffer> currentOffers = entry.getValue().getTransportOffers();
				if(currentOffers.isEmpty())
				{
					costGain = (int)offerToShare.getCost();
				}
				else
				{
					TransportOffer currentTopOffer = currentOffers.get(0);
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
		
		ITransportServiceRecord serviceRecordOfSharingUser = mTransportServiceRecords.get(sharingUser);
		if(selectedUser != null)
		{
			serviceRecordOfSharingUser.removeTransportOffer(offerToShare);
			
			ITransportServiceRecord serviceRecordOfSelectedUser = mTransportServiceRecords.get(selectedUser);
			offerToShare.setTransportServiceRequest(serviceRecordOfSelectedUser.getTransportServiceRequest());
			serviceRecordOfSelectedUser.addTransportOffer(offerToShare);
			
			return true;
		}
		else
		{
			ITransportServiceRequest request = serviceRecordOfSharingUser.getTransportServiceRequest();
			TransportPreference pref = request.getTransportPreference();
			offerToShare.applyTransportPreference(pref);
			
			return false;
		}
	}
	
	private void sendOffers()
	{
		logger.info("sendOffers()");
		
		Iterator<Map.Entry<NetworkAddress, ITransportServiceRecord>> iterator = mTransportServiceRecords.
				entrySet().iterator();
		while(iterator.hasNext())
		{
			iterator.next().getValue().sendTransportOffers();
		}
	}
	
	private NetworkAddress getUserNearestTo(Set<NetworkAddress> userAddresses, Location location)
	{
		NetworkAddress nearestUser = null;
		double minDistance = Integer.MAX_VALUE;
		for(NetworkAddress userAddress: userAddresses)
		{
			double distance = mTransportServiceRecords.get(userAddress).
					getTransportServiceRequest().getStartLocation().distanceTo(location);
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
