package gui.pieChartWindow;

import gui.Window;

import java.util.Map;

public class PieChartWindow extends Window
{
	private final String LAYOUTS_PATH = "../../layouts/";
	private final String PIE_CHART = LAYOUTS_PATH + "PieChart.fxml";
	
	private PieChartController mPieChartController;
	private Map<String, Double> mPieData;
	
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
	
	public void setData(Map<String, Double> data)
	{
		assert(data != null);
		
		mPieData = data;
	}
}
