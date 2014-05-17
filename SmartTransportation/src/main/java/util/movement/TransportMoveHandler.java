package util.movement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import uk.ac.imperial.presage2.core.Action;
import uk.ac.imperial.presage2.core.environment.ActionHandler;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.environment.ServiceDependencies;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.location.CannotSeeAgent;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.LocationService;
import uk.ac.imperial.presage2.util.location.Move;
import uk.ac.imperial.presage2.util.location.MoveHandler;
import uk.ac.imperial.presage2.util.location.area.Area;
import uk.ac.imperial.presage2.util.location.area.AreaService;
import uk.ac.imperial.presage2.util.location.area.EdgeException;
import uk.ac.imperial.presage2.util.location.area.HasArea;

@ServiceDependencies({ LocationService.class, AreaService.class })
public class TransportMoveHandler implements ActionHandler
{
	private final Logger logger = Logger.getLogger(MoveHandler.class);
	
	final protected Area mArea;
	final protected EnvironmentSharedStateAccess mSharedState;
	private static  EnvironmentServiceProvider mServiceProvider;
	private static LocationService mLocationService;
	private static Map<UUID, List<Movement>> mAgentMovements;
	private static Map<UUID, List<Movement>> mMovingAgents;
	private static int mSimulationTime;
	
	@Inject
	public TransportMoveHandler(HasArea environment,
			EnvironmentServiceProvider serviceProvider,
			EnvironmentSharedStateAccess sharedState)
			throws UnavailableServiceException 
	{
		mArea = environment.getArea();
		mServiceProvider = serviceProvider;
		mSharedState = sharedState;
		mLocationService = serviceProvider.getEnvironmentService(LocationService.class);
		mAgentMovements = new ConcurrentHashMap<UUID, List<Movement>>();
		mMovingAgents = new ConcurrentHashMap<UUID, List<Movement>>();
		mSimulationTime = 0;
	}
	
	public static Map<UUID, List<Movement>> getAgentsMovements()
	{
		return mAgentMovements;
	}
	
	public static void incrementTime() 
	{
		++mSimulationTime;
		
		processMovingAgents();
	}
	
	@Override
	public boolean canHandle(Action action) 
	{
		return action instanceof TransportMove;
	}
	
	@Override
	public Input handle(Action action, UUID agentID)
			throws ActionHandlingException 
	{
		if(handleAction(action, agentID))
		{
			return null;
		}
		else
		{
			throw new ActionHandlingException(
					"MoveHandler was asked to handle non Move action!");
		}
	}
	
	private boolean handleAction(Action action, UUID agentID) throws ActionHandlingException
	{
		if(action instanceof TransportMove)
		{
			handleTransportMove((TransportMove)action, agentID);
			return true;
		}
		return false;
	}
	
	private void handleTransportMove(TransportMove move, UUID agentID) 
			throws ActionHandlingException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Handling transport move " + move.getTargetLocation() + " from " + agentID);
		}
		
		Location currentLocation = getCurrentLocation(agentID);
		moveAgent(agentID, currentLocation, move.getTargetLocation(),
				move.getTimePerUnitDistance());
	}
	
	private Location getCurrentLocation(UUID actor) throws ActionHandlingException
	{
		try 
		{
			return mLocationService.getAgentLocation(actor);
		} 
		catch (CannotSeeAgent e) 
		{
			throw new ActionHandlingException(e);
		}
	}
	
	private void moveAgent(UUID agentID, Location currentLocation, 
			Location targetLocation, int timeTakenPerUnitDistance) 
			throws ActionHandlingException
	{
		if (targetLocation.in(mArea) == false) 
		{
			try 
			{
				Move currentMove = new Move(targetLocation);
				targetLocation = new Location(currentLocation.
						add(mArea.getValidMove(currentLocation, currentMove)));
			} 
			catch (EdgeException e) 
			{
				throw new ActionHandlingException(e);
			}
		}
		
		if(timeTakenPerUnitDistance == 1)
		{
			changeAgentLocation(agentID, targetLocation, 
					timeTakenPerUnitDistance);
		}
		else
		{
			int moveStartTime = mSimulationTime;
			List<Movement> pendingMovements = mMovingAgents.get(agentID);
			if(pendingMovements == null)
			{
				pendingMovements = new ArrayList<Movement>();
				mMovingAgents.put(agentID, pendingMovements);
			}
			else if(pendingMovements.size() > 0)
			{
				int previousMovementIndex = pendingMovements.size() - 1;
				Movement previousMovement = pendingMovements.get(previousMovementIndex);
				moveStartTime = previousMovement.getStartTime() + 
						previousMovement.getTimeTakenPerUnitDistance();
			}
			pendingMovements.add(new Movement
					(targetLocation, timeTakenPerUnitDistance, moveStartTime));
		}
	}
	
	private static void processMovingAgents()
	{
		if(mMovingAgents.isEmpty() == false)
		{
			Iterator<Map.Entry<UUID, List<Movement>>> iterator = mMovingAgents.entrySet().iterator();
			while(iterator.hasNext())
			{
				Map.Entry<UUID, List<Movement>> entry = iterator.next();
				List<Movement> pendingMovements = entry.getValue();
				if(pendingMovements.isEmpty() == false)
				{
					Movement movement = pendingMovements.get(0);
					int timeElapsed = movement.getStartTime() + movement.getTimeTakenPerUnitDistance();
					if(timeElapsed <= mSimulationTime)
					{
						changeAgentLocation(entry.getKey(), movement.getLocation(), 
								movement.getTimeTakenPerUnitDistance());
						
						pendingMovements.remove(0);
						if(pendingMovements.isEmpty())
						{
							iterator.remove();
						}
					}
				}
				else 
				{
					iterator.remove();
				}
			}
		}
	}
	
	private static void changeAgentLocation(UUID agentID, Location targetLocation,
			int timeTakenPerUnitDistance)
	{
		mLocationService.setAgentLocation(agentID, targetLocation);
		addMovementForAgent(agentID, targetLocation, timeTakenPerUnitDistance);
	}
	
	private static void addMovementForAgent(UUID agentID, Location location, 
			int timeTakenPerUnitDistance)
	{
		List<Movement> movements = mAgentMovements.get(agentID);
		if(movements == null)
		{
			movements = new ArrayList<Movement>();
			mAgentMovements.put(agentID, movements);
		}
		movements.add(new Movement(location, timeTakenPerUnitDistance, mSimulationTime));
	}
}
