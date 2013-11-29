package map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import uk.ac.imperial.presage2.util.location.area.Area;
import uk.ac.imperial.presage2.util.location.area.HasArea;

public class CityMap extends Area
{
	private int[][] mMapConfiguration;
	public CityMap(int x, int y, int[][] mapConfig) 
	{
		super(x, y, 1);
		
		mMapConfiguration = mapConfig;
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
		private int mWidth;
		private int mHeight;
		private int[][] mMapConfiguration;
		private CityMap mCityMap;
		
		public Bind(int width, int height, int[][] mapConfiguration) 
		{
			super();
			mWidth = width;
			mHeight = height;
			mMapConfiguration = mapConfiguration;
		}
		
		public static Bind cityMap2D(final int width, final int height, final int[][] mapConfig) 
		{
			return new Bind(width, height, mapConfig);
		}
		
		@Override
		protected void configure() 
		{
//			bind(HasArea.class).to(CityMap.class);
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
				mCityMap = new CityMap(mWidth, mHeight, mMapConfiguration);
			}
			return mCityMap;
		}
	}	
}
