package gui.charts.userDataTable;

import gui.charts.Chart;
import javafx.collections.ObservableList;

public class UserTableWindow extends Chart
{
	private final static String USER_TABLE = Chart.CHARTS_PATH + "UserTable.fxml";
	
	private UserTableController mUserTableController;
	private ObservableList<UserTableData> mTableData;
	
	public void show()
	{
		super.show();
		if(mStage.isShowing() == false)
		{
			mUserTableController = (UserTableController)loadScene(USER_TABLE);
			if(mTableData != null)
			{
				mUserTableController.setData(mTableData);
			}
			mStage.centerOnScreen();
			mStage.show();	
		}
	}
	
	public void setData(ObservableList<UserTableData> data)
	{
		assert(data != null);
		
		mTableData = data;
	}
}
