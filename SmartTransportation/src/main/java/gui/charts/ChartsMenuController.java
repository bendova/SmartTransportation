package gui.charts;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

public class ChartsMenuController extends Parent implements Initializable
{
	@FXML
	private Pane container;
	@FXML
	private Button showTransportMethodsUsed; 
	@FXML
	private Button showUserTransportResults; 
	@FXML
	private Button showUserDataTable; 
	
	private Callback<Void, Void> mOnShowTransportMethodUsed;
	private Callback<Void, Void> mOnShowUserTransportResults;
	private Callback<Void, Void> mOnShowUserDataTable;
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		System.out.println("ChartsMenuController::initialize()");
		
		showTransportMethodsUsed.setOnAction(new EventHandler<ActionEvent>() 
		{
			@Override
			public void handle(ActionEvent event) 
			{
				if(mOnShowTransportMethodUsed != null)
				{
					mOnShowTransportMethodUsed.call(null);
				}
			}
		});
		showUserTransportResults.setOnAction(new EventHandler<ActionEvent>() 
		{
			@Override
			public void handle(ActionEvent event) 
			{
				if(mOnShowUserTransportResults != null)
				{
					mOnShowUserTransportResults.call(null);
				}
			}
		});
		showUserDataTable.setOnAction(new EventHandler<ActionEvent>() 
		{
			@Override
			public void handle(ActionEvent event) 
			{
				if(mOnShowUserDataTable != null)
				{
					mOnShowUserDataTable.call(null);
				}
			}
		});
	}
	
	public void setOnShowTransportMethodUsed(Callback<Void, Void> callback)
	{
		assert(callback != null);
		
		mOnShowTransportMethodUsed = callback;
	}
	public void setOnShowUserTransportResults(Callback<Void, Void> callback)
	{
		assert(callback != null);
		
		mOnShowUserTransportResults = callback;
	}
	public void setOnShowUserDataTable(Callback<Void, Void> callback)
	{
		assert(callback != null);
		
		mOnShowUserDataTable = callback;
	}
	
	public Pane getContainer()
	{
		return container;
	}
}
