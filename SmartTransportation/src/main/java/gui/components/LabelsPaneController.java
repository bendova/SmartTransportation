package gui.components;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

public class LabelsPaneController extends Parent implements Initializable
{
	@FXML
	public Pane root;
	@FXML
	public Pane labelsContainer;
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		System.out.println("LabelsPaneController::initialize()");
		
	}
	
	public Pane getContainer()
	{
		return root;
	}
}
