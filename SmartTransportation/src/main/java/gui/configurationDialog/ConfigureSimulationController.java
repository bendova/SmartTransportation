package gui.configurationDialog;

import java.net.URL;
import java.util.List;
import java.util.Observable;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class ConfigureSimulationController extends StackPane implements Initializable
{
	@FXML
	private Button startButton;
	
	// GUI parameters
	@FXML
	private TextField timeStepDurationTF;
	@FXML
	private TextField pixelsPerAreaPointTF;
	
	// Simulation parameters
	@FXML
	private TextField timeStepsCountTF;
	@FXML
	private TextField areaSizeTF;
	
	// Transport methods available
	@FXML
	private CheckBox walkingCheckBox;
	@FXML
	private CheckBox taxiesCheckBox;
	@FXML
	private CheckBox busesCheckBox;
	
	// User parameters
	@FXML
	private TextField usersCountTF;
	@FXML
	private ChoiceBox<String> transportPreferenceChoiceBox;
	@FXML
	private ChoiceBox<String> timeContraintChoiceBox;
	
	// Buses parameters
	@FXML
	private TextField busRoutesCountTF;
	@FXML
	private TextField busesCountTF;
	@FXML
	private GridPane busParametersGridPane;
	
	// Taxies parameters
	@FXML
	private TextField taxiStationsCountTF;
	@FXML
	private TextField taxiesCountTF;
	@FXML
	private GridPane taxiParametersGridPane;
	
	private Callback<SimulationConfiguration, Void> mOnStart;
	
	public void setOnStartCallback(Callback<SimulationConfiguration, Void> onStart)
	{
		mOnStart = onStart;
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) 
	{
		System.out.println("ConfigureSimulationController::initialize()");
		
		busParametersGridPane.disableProperty().bind(busesCheckBox.selectedProperty().not());
		taxiParametersGridPane.disableProperty().bind(taxiesCheckBox.selectedProperty().not());
	}
	
	public void setTransportAllocationTypes(List<String> types)
	{
		ObservableList<String> items = transportPreferenceChoiceBox.getItems();
		items.clear();
		items.addAll(types);
		transportPreferenceChoiceBox.getSelectionModel().selectFirst();
	}
	
	public void setsTimeConstraints(List<String> constraints)
	{
		ObservableList<String> items = timeContraintChoiceBox.getItems();
		items.clear();
		items.addAll(constraints);
		timeContraintChoiceBox.getSelectionModel().selectFirst();
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
		int taxiesCount = 0;
		int taxiStationsCount = 0;
		if(taxiesCheckBox.isSelected())
		{
			taxiesCount = getValue(taxiesCountTF.getText());
			taxiStationsCount = getValue(taxiStationsCountTF.getText());
		}
		int busesCount = 0;
		int busRoutesCount = 0;
		if(busesCheckBox.isSelected())
		{
			busesCount = getValue(busesCountTF.getText());
			busRoutesCount = getValue(busRoutesCountTF.getText());
		}
		int transportAllocationIndex = transportPreferenceChoiceBox.getSelectionModel().getSelectedIndex();
		int timeConstraintIndex = timeContraintChoiceBox.getSelectionModel().getSelectedIndex();
		
		SimulationConfiguration config = new SimulationConfiguration(timeStepDuration, 
				timeStepsCount, areaSize, pixelsPerPoint, usersCount, taxiesCount, 
				taxiStationsCount, busesCount, busRoutesCount, transportAllocationIndex, timeConstraintIndex);
		return config;
	}
	
	private int getValue(String text)
	{
		int value = Integer.parseInt(text);
		return (value > 0) ? value : 0;
	}
}
