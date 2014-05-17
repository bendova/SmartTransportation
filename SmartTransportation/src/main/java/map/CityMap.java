package map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.area.Area;
import uk.ac.imperial.presage2.util.location.area.HasArea;

public class CityMap extends Area
{
	private int[][] mMapConfiguration;
	public CityMap(int[][] mapConfig) 
	{
		super(mapConfig.length, mapConfig[0].length, 1);
		
		mMapConfiguration = mapConfig;
	}
	
	public List<Location> getPath(Location start, Location destination)
	{
		Set<Location> evaluatedLocations = new HashSet<Location>();
		Map<Location, Location> traveledPaths = new HashMap<Location, Location>();
		Set<Location> locationsToEvaluate = new HashSet<Location>();
		locationsToEvaluate.add(start);
		
		Map<Location, Integer> globalCostMap = new HashMap<Location, Integer>();
		globalCostMap.put(start, 0);
		Map<Location, Integer> estimatedCostMap = new HashMap<Location, Integer>();
		estimatedCostMap.put(start, Integer.valueOf((int)start.distanceTo(destination)));
		
		while(locationsToEvaluate.isEmpty() == false)
		{
			Location currentLocation = null;
			int minCost = Integer.MAX_VALUE;
			for (Iterator<Location> iterator = locationsToEvaluate.iterator(); iterator.hasNext();)
			{
				Location location = iterator.next();
				if(estimatedCostMap.get(location) < minCost)
				{
					minCost = estimatedCostMap.get(location);
					currentLocation = location;
				}
			}
			
			if(currentLocation.equals(destination))
			{
				List<Location> pathList = reconstructPath(traveledPaths, destination);
				pathList.remove(0);
				return pathList;
			}
			
			locationsToEvaluate.remove(currentLocation);
			evaluatedLocations.add(currentLocation);
			// evaluate the neighbors
			for (Iterator<Location> iterator2 = getNeighboringLocations(currentLocation).iterator(); 
					iterator2.hasNext();) 
			{
				Location neighbor = iterator2.next();
				int cost = globalCostMap.get(currentLocation) + (int)currentLocation.distanceTo(neighbor);
				int estimatedTotalCost = cost + (int)neighbor.distanceTo(destination);
				
				if(evaluatedLocations.contains(neighbor) && (estimatedTotalCost >= estimatedCostMap.get(neighbor)))
				{
					continue;
				}
				
				if((locationsToEvaluate.contains(neighbor) == false) || (estimatedTotalCost < estimatedCostMap.get(neighbor)))
				{
					traveledPaths.put(neighbor, currentLocation);
					globalCostMap.put(neighbor, cost);
					estimatedCostMap.put(neighbor, estimatedTotalCost);
					if(locationsToEvaluate.contains(neighbor) == false)
					{
						locationsToEvaluate.add(neighbor);
					}
				}
			}
		}
		return new ArrayList<Location>();
	}
	
	private List<Location> getNeighboringLocations(Location location)
	{
		List<Location> neighbors = new ArrayList<Location>();
		
		int x = (int) location.getX(); 
		int y = (int) location.getY(); 
		if((isValidLocation(--x, y)))
		{
			neighbors.add(new Location(x, y));
		}
		++x;
		if(isValidLocation(++x, y))
		{
			neighbors.add(new Location(x, y));
		}
		--x;
		if((isValidLocation(x, --y)))
		{
			neighbors.add(new Location(x, y));
		}
		++y;
		if(isValidLocation(x, ++y))
		{
			neighbors.add(new Location(x, y));
		}
		--y;
		
		/*
		if((isValidLocation(--x, ++y)))
		{
			neighbors.add(new Location(x, y));
		}
		++x;
		--y;
		if(isValidLocation(++x, ++y))
		{
			neighbors.add(new Location(x, y));
		}
		--x;
		--y;
		if((isValidLocation(--x, --y)))
		{
			neighbors.add(new Location(x, y));
		}
		++x;
		++y;
		if(isValidLocation(++x, --y))
		{
			neighbors.add(new Location(x, y));
		}
		--x;
		++y;
		*/
		return neighbors;
	}
	
	private List<Location> reconstructPath(Map<Location, Location> traveledPaths, 
			Location currentLocation)
	{
		if(traveledPaths.containsKey(currentLocation))
		{
			List<Location> path = reconstructPath(traveledPaths, traveledPaths.get(currentLocation));
			path.add(currentLocation);
			return path;
		}
		else 
		{
			List<Location> path = new ArrayList<Location>();
			path.add(currentLocation);
			return path;
		}
	}
	
	public boolean isValidLocation(int x, int y)
	{
		if((x < 0) || (x >= mMapConfiguration.length))
		{
			return false;
		}
		if((y < 0) || (y >= mMapConfiguration[0].length))
		{
			return false;
		}
		return (mMapConfiguration[x][y] == 0);
	}
	
	public static class Bind extends AbstractModule 
	{
		private int[][] mMapConfiguration;
		private CityMap mCityMap;
		
		public Bind(int[][] mapConfiguration) 
		{
			super();
			mMapConfiguration = mapConfiguration;
		}
		
		public static Bind cityMap2D(final int[][] mapConfig) 
		{
			return new Bind(mapConfig);
		}
		
		@Override
		protected void configure() 
		{
			//bind(HasArea.class).to(CityMap.class);
		}
		
		@Provides
		HasArea provideArea()
		{
			return getCityMap();
		}
		
		@Provides
		CityMap provideCityMap()
		{
			return getCityMap();
		}
		
		private CityMap getCityMap()
		{
			if(mCityMap == null)
			{
				mCityMap = new CityMap(mMapConfiguration);
			}
			return mCityMap;
		}
	}	
}
