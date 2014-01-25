package gui.configurationDialog;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class ConfigureSimulationController extends StackPane implements Initializable
{
	@FXML
	private GridPane gridPane;
	@FXML
	private Button startButton;
	@FXML
	private TextField timeStepDurationTF;
	@FXML
	private TextField timeStepsCountTF;
	@FXML
	private TextField areaSizeTF;
	@FXML
	private TextField pixelsPerAreaPointTF;
	@FXML
	private TextField usersCountTF;
	@FXML
	private TextField taxiesCountTF;
	@FXML
	private TextField taxiStationsCountTF;
	@FXML
	private TextField busesCountTF;
	
	private Callback<SimulationConfiguration, Void> mOnStart;
	
	public void setOnStartCallback(Callback<SimulationConfiguration, Void> onStart)
	{
		mOnStart = onStart;
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) 
	{
		System.out.println("ConfigureSimulationController::initialize()");
	}
	
	@FXML
	private void onStart()
	{
		System.out.println("ConfigureSimulationController::onStart()");
		
		if(mOnStart != null)
		{
			mOnStart.call(getConfiguration());
		}
	}
	
	private SimulationConfiguration getConfiguration()
	{
		int timeStepDuration = getValue(timeStepDurationTF.getText());
		int pixelsPerPoint = getValue(pixelsPerAreaPointTF.getText());
		
		int timeStepsCount = getValue(timeStepsCountTF.getText());
		int areaSize = getValue(areaSizeTF.getText());
		int usersCount = getValue(usersCountTF.getText());
		int taxiesCount = getValue(taxiesCountTF.getText());
		int taxiStationsCount = getValue(taxiStationsCountTF.getText());
		int busesCount = getValue(busesCountTF.getText());
		
		SimulationConfiguration config = new SimulationConfiguration(timeStepDuration, 
				timeStepsCount, areaSize, pixelsPerPoint, usersCount, taxiesCount, 
				taxiStationsCount, busesCount);
		return config;
	}
	
	private int getValue(String text)
	{
		int value = Integer.parseInt(text);
		return (value > 0) ? value : 0;
	}
}
