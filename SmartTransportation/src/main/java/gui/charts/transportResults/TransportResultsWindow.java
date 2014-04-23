package gui.charts.transportResults;

import gui.charts.Chart;

import java.util.TreeMap;

public class TransportResultsWindow extends Chart
{
	private final static String AREA_CHART = Chart.LAYOUTS_PATH + "Chart.fxml";
	
	private TransportResultsController mAreaChartController;
	private TreeMap<Number, Number> mChartData;

	public void show()
	{
		super.show();
		if(mStage.isShowing() == false)
		{
			mAreaChartController = (TransportResultsController)loadScene(AREA_CHART);
			if(mChartData != null)
			{
				mAreaChartController.setAreaChartData(mChartData);
			}
			mStage.centerOnScreen();
			mStage.show();
		}
	}
	
	public void setData(TreeMap<Number, Number> data)
	{
		assert(data != null);
		
		mChartData = data;
	}
}
