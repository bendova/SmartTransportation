package gui.charts.transportMethodsUsed;

import gui.charts.Chart;

import java.util.Map;

public class TransportMethodsUsedWindow extends Chart
{
	private final static String PIE_CHART = Chart.LAYOUTS_PATH + "PieChart.fxml";
	
	private TransportMethodsUsedController mPieChartController;
	private Map<String, Double> mPieData;
	
	public void show()
	{
		super.show();
		if(mStage.isShowing() == false)
		{
			mPieChartController = (TransportMethodsUsedController)loadScene(PIE_CHART);
			if(mPieData != null)
			{
				mPieChartController.setPieChartData(mPieData);
			}
			mStage.centerOnScreen();
			mStage.show();	
		}
	}
	
	public void setData(Map<String, Double> data)
	{
		assert(data != null);
		
		mPieData = data;
	}
}
