package gui.charts.pieChart;

import gui.charts.Chart;

import java.util.Map;

public class PieChartWindow extends Chart
{
	private final static String PIE_CHART = Chart.CHARTS_PATH + "PieChart.fxml";
	
	private PieChartController mPieChartController;
	private Map<String, Integer> mPieData;
	
	public void show()
	{
		super.show();
		if(mStage.isShowing() == false)
		{
			mPieChartController = (PieChartController)loadScene(PIE_CHART);
			if(mPieData != null)
			{
				mPieChartController.setPieChartData(mPieData);
			}
			mStage.centerOnScreen();
			mStage.show();	
		}
	}
	
	public void setData(Map<String, Integer> data)
	{
		assert(data != null);
		
		mPieData = data;
	}
}
