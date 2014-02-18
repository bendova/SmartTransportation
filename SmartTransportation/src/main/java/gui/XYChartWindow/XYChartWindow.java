package gui.XYChartWindow;

import gui.Window;
import java.util.TreeMap;

public class XYChartWindow extends Window
{
	private final String LAYOUTS_PATH = "../../layouts/";
	private final String AREA_CHART = LAYOUTS_PATH + "Chart.fxml";
	
	private XYChartController mAreaChartController;
	private TreeMap<Number, Number> mChartData;

	public void show()
	{
		super.show();
		if(mStage.isShowing() == false)
		{
			mAreaChartController = (XYChartController)loadScene(AREA_CHART);
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
