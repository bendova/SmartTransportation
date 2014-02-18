package gui.tableWindow;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

public class UserTableController extends Parent implements Initializable
{
	@FXML
	private StackPane container;
	
	private TableView<UserTableData> mTableView;
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		System.out.println("UserTableController::initialize()");
	}
	
	public void setData(ObservableList<UserTableData> userTableDataList)
	{
		TableColumn<UserTableData, String> nameColumn = new TableColumn<UserTableData, String>();
		nameColumn.setText("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, String>("name"));
		
		TableColumn<UserTableData, Boolean> hasReachedDestinationColumn = new TableColumn<UserTableData, Boolean>();
		hasReachedDestinationColumn.setText("Has reached destination");
		hasReachedDestinationColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, Boolean>("hasReachedDestination"));
		
		TableColumn<UserTableData, Double> travelTimeColumn = new TableColumn<UserTableData, Double>();
		travelTimeColumn.setText("Travel time");
		travelTimeColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, Double>("travelTime"));
		
		TableColumn<UserTableData, Double> travelTimeTargetColumn = new TableColumn<UserTableData, Double>();
		travelTimeTargetColumn.setText("Travel time target");
		travelTimeTargetColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, Double>("travelTimeTarget"));
		
		TableColumn<UserTableData, String> transportPreferenceColumn = new TableColumn<UserTableData, String>();
		transportPreferenceColumn.setText("Transport preference");
		transportPreferenceColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, String>("transportPreference"));
		
		TableColumn<UserTableData, String> transportModeUsedColumn = new TableColumn<UserTableData, String>();
		transportModeUsedColumn.setText("Transport mode used");
		transportModeUsedColumn.setCellValueFactory(new PropertyValueFactory<UserTableData, String>("transportModeUsed"));
		
		mTableView = new TableView<UserTableData>();
		mTableView.setItems(userTableDataList);
		mTableView.getColumns().addAll(nameColumn, hasReachedDestinationColumn, 
				travelTimeColumn, travelTimeTargetColumn, transportPreferenceColumn, transportModeUsedColumn);
		mTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		container.getChildren().add(mTableView);
	}
}
