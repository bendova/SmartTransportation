package gui.screens.configurationScreen;

import java.net.URL;
import java.util.List;
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
	@FXML
	private CheckBox saveConfigurationCheckBox;
	
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
	
	public void setConfiguration(SimulationConfiguration config)
	{
		timeStepDurationTF.setText(Integer.toString(config.getTimeStepDuration()));
		pixelsPerAreaPointTF.setText(Integer.toString(config.getPixelsPerAreaPoint()));
		
		timeStepsCountTF.setText(Integer.toString(config.getTimeStepsCount()));
		areaSizeTF.setText(Integer.toString(config.getAreaSize()));
		
		usersCountTF.setText(Integer.toString(config.getUsersCount()));
		walkingCheckBox.setSelected(config.isWalkingEnabled());
		
		taxiesCheckBox.setSelected(config.areTaxiesEnabled());
		taxiesCountTF.setText(Integer.toString(config.getTaxiesCount()));
		taxiStationsCountTF.setText(Integer.toString(config.getTaxiStationsCount()));
		
		busesCheckBox.setSelected(config.areBusesEnabled());
		busesCountTF.setText(Integer.toString(config.getBusesCount()));
		busRoutesCountTF.setText(Integer.toString(config.getBusRoutesCount()));
		
		transportPreferenceChoiceBox.getSelectionModel().select(config.getTransportAllocationIndex());
		timeContraintChoiceBox.getSelectionModel().select(config.getTimeConstraintIndex());
		
		saveConfigurationCheckBox.setSelected(config.getSaveConfiguration());
	}
	
	private SimulationConfiguration getConfiguration()
	{
		int timeStepDuration = getValue(timeStepDurationTF.getText());
		int pixelsPerPoint = getValue(pixelsPerAreaPointTF.getText());
		
		int timeStepsCount = getValue(timeStepsCountTF.getText());
		int areaSize = getValue(areaSizeTF.getText());
		int usersCount = getValue(usersCountTF.getText());
		boolean isWalkingEnabled = walkingCheckBox.isSelected();
		int taxiesCount = 0;
		int taxiStationsCount = 0;
		boolean areTaxiesEnabled = taxiesCheckBox.isSelected();
		if(areTaxiesEnabled)
		{
			taxiesCount = getValue(taxiesCountTF.getText());
			taxiStationsCount = getValue(taxiStationsCountTF.getText());
		}
		int busesCount = 0;
		int busRoutesCount = 0;
		boolean areBusesEnabled = busesCheckBox.isSelected();
		if(areBusesEnabled)
		{
			busesCount = getValue(busesCountTF.getText());
			busRoutesCount = getValue(busRoutesCountTF.getText());
		}
		int transportAllocationIndex = transportPreferenceChoiceBox.getSelectionModel().getSelectedIndex();
		int timeConstraintIndex = timeContraintChoiceBox.getSelectionModel().getSelectedIndex();
		
		boolean saveConfiguration = saveConfigurationCheckBox.isSelected();
		
		SimulationConfiguration config = new SimulationConfiguration()
						.setTimeStepDuration(timeStepDuration)
						.setTimeStepsCount(timeStepsCount)
						.setAreaSize(areaSize)
						.setPixelsPerAreaPoint(pixelsPerPoint)
						.setUsersCount(usersCount)
						.setAreTaxiesEnabled(areTaxiesEnabled)
						.setTaxiesCount(taxiesCount)
						.setTaxiStationsCount(taxiStationsCount)
						.setAreBusesEnabled(areBusesEnabled)
						.setBusesCount(busesCount)
						.setBusRoutesCount(busRoutesCount)
						.setIsWalkingEnabled(isWalkingEnabled)
						.setTransportAllocationMethodIndex(transportAllocationIndex)
						.setTimeContraintTypeIndex(timeConstraintIndex)
						.setSaveConfiguration(saveConfiguration);
		return config;
	}
	
	private int getValue(String text)
	{
		int value = Integer.parseInt(text);
		return (value > 0) ? value : 0;
	}
}
