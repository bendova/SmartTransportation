package gui.tableWindow;

import gui.Window;
import javafx.collections.ObservableList;

public class UserTableWindow extends Window
{
	private final String LAYOUTS_PATH = "../../layouts/";
	private final String USER_TABLE = LAYOUTS_PATH + "UserTable.fxml";
	
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
