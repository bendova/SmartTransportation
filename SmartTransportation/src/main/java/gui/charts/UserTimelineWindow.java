package gui.charts;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dataStores.userData.IUserData;
import dataStores.userData.UserEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class UserTimelineWindow extends Stage
{
	public UserTimelineWindow(Map<UUID, IUserData> userDataStores)
	{
		Scene scene = new Scene(createTabPane(userDataStores));
		setScene(scene);
		centerOnScreen();
		setTitle("User Timeline");
	}
	
	private TabPane createTabPane(Map<UUID, IUserData> userDataStores)
	{
		TabPane tabPane = new TabPane();
		ObservableList<Tab> tabsList = tabPane.getTabs();
		Iterator<Map.Entry<UUID, IUserData>> iterator = userDataStores.entrySet().iterator();
		while(iterator.hasNext())
		{
			IUserData data = iterator.next().getValue();
			tabsList.add(createTab(data.getName(), data.getUserEvents()));
		}
		
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		return tabPane;
	}
	
	private Tab createTab(String tabName, List<UserEvent> events)
	{
		Tab tab = new Tab(tabName);
		tab.setContent(createTable(events));
		tab.setText(tabName);
		return tab;
	}
	
	private TableView<UserEvent> createTable(List<UserEvent> events)
	{
		TableColumn<UserEvent, Integer> timestepColumn = new TableColumn<UserEvent, Integer>();
		timestepColumn.setMinWidth(80);
		timestepColumn.setText("Timestep");
		timestepColumn.setCellValueFactory(new PropertyValueFactory<UserEvent, Integer>("timestep"));
		
		TableColumn<UserEvent, String> eventNameColumn = new TableColumn<UserEvent, String>();
		eventNameColumn.setMinWidth(200);
		eventNameColumn.setText("Event Name");
		eventNameColumn.setCellValueFactory(new PropertyValueFactory<UserEvent, String>("eventName"));
		
		TableColumn<UserEvent, String> eventDetailsColumn = new TableColumn<UserEvent, String>();
		eventDetailsColumn.setMinWidth(500);
		eventDetailsColumn.setText("Event Details");
		eventDetailsColumn.setCellValueFactory(new PropertyValueFactory<UserEvent, String>("eventDetails"));
		
		TableColumn<UserEvent, String> userStateColumn = new TableColumn<UserEvent, String>();
		userStateColumn.setMinWidth(200);
		userStateColumn.setText("User state");
		userStateColumn.setCellValueFactory(new PropertyValueFactory<UserEvent, String>("state"));
		
		TableColumn<UserEvent, String> locationColumn = new TableColumn<UserEvent, String>();
		locationColumn.setMinWidth(80);
		locationColumn.setText("Location");
		locationColumn.setCellValueFactory(new PropertyValueFactory<UserEvent, String>("location"));
		
		TableView<UserEvent> tableView = new TableView<UserEvent>();
		tableView.getColumns().addAll(timestepColumn, eventNameColumn, eventDetailsColumn, 
				userStateColumn, locationColumn);
		tableView.setItems(FXCollections.observableArrayList(events));
		return tableView;
	}
}
