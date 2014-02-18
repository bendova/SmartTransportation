package gui;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public class ChartsMenuController extends Parent implements Initializable
{
	@FXML
	public Pane container;
	@FXML
	public Button showTransportMethodsUsed; 
	@FXML
	public Button showUserTransportResults; 
	@FXML
	public Button showUserDataTable; 
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		System.out.println("ChartsMenuController::initialize()");
	}
}
