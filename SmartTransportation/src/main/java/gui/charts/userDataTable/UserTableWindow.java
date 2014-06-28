package gui.charts.userDataTable;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class UserTableWindow extends Stage
{
	public UserTableWindow(ObservableList<UserTableData> data)
	{
		assert(data != null);
		
		Scene scene = new Scene(createTable(data));
		setScene(scene);
		centerOnScreen();
		setTitle("User Data Table");
	}
	
	public TableView<UserTableData> createTable(ObservableList<UserTableData> userTableDataList)
	{
		TableColumn<UserTableData, String> nameColumn = new TableColumn<UserTableData, String>();
		nameColumn.setMinWidth(100);
		nameColumn.setText("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, String>("name"));
		
		TableColumn<UserTableData, Boolean> hasReachedDestinationColumn = new TableColumn<UserTableData, Boolean>();
		hasReachedDestinationColumn.setMinWidth(200);
		hasReachedDestinationColumn.setText("Has reached destination");
		hasReachedDestinationColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, Boolean>("hasReachedDestination"));
		
		TableColumn<UserTableData, Boolean> onTimeColumn = new TableColumn<UserTableData, Boolean>();
		onTimeColumn.setMinWidth(100);
		onTimeColumn.setText("On Time");
		onTimeColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, Boolean>("hasReachedDestinationOnTime"));
		
		TableColumn<UserTableData, Double> travelTimeColumn = new TableColumn<UserTableData, Double>();
		travelTimeColumn.setMinWidth(100);
		travelTimeColumn.setText("Travel time");
		travelTimeColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, Double>("travelTime"));
		
		TableColumn<UserTableData, Double> travelTimeTargetColumn = new TableColumn<UserTableData, Double>();
		travelTimeTargetColumn.setMinWidth(140);
		travelTimeTargetColumn.setText("Travel time target");
		travelTimeTargetColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, Double>("travelTimeTarget"));
		
		TableColumn<UserTableData, String> transportPreferenceColumn = new TableColumn<UserTableData, String>();
		transportPreferenceColumn.setMinWidth(200);
		transportPreferenceColumn.setText("Transport preference");
		transportPreferenceColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, String>("transportPreference"));
		
		TableColumn<UserTableData, String> transportModeUsedColumn = new TableColumn<UserTableData, String>();
		transportModeUsedColumn.setMinWidth(200);
		transportModeUsedColumn.setText("Transport mode used");
		transportModeUsedColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, String>("transportModeUsed"));
		
		TableView<UserTableData> mTableView = new TableView<UserTableData>();
		mTableView.setItems(userTableDataList);
		mTableView.getColumns().addAll(nameColumn, hasReachedDestinationColumn, onTimeColumn,
				travelTimeColumn, travelTimeTargetColumn, transportPreferenceColumn, transportModeUsedColumn);
		mTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		return mTableView;
	}
}
