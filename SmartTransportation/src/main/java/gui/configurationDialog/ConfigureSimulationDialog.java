package gui.configurationDialog;

import javafx.event.*;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

public class ConfigureSimulationDialog extends Stage
{
	private final SubmitConfigurationHandler mSubmitHandler;
	private final String DURATION_DEFAULT = "50";
	private final String AREA_SIZE_DEFAULT = "10";
	private final String USERS_COUNT_DEFAULT = "20";
	private final String TAXIES_COUNT_DEFAULT = "3";
	private final String TAXI_STATION_COUNT_DEFAULT = "1";
	
	private Group mRoot;
	
	private TextField mDurationTextField;
	private TextField mAreaSizeTextField;
	private TextField mUsersCountTextField;
	private TextField mTaxiesCountTextField;
	private TextField mTaxiStationsCountTextField;
	
	public ConfigureSimulationDialog(Stage owner, final SubmitConfigurationHandler submitHandler)
	{
		super();
		initOwner(owner);
		mSubmitHandler = submitHandler;
		initLayout();
	}
	
	private void initLayout()
	{
		Modality modalityType = Modality.APPLICATION_MODAL;
		initModality(modalityType);
		setOpacity(1);
		setTitle("Configure Simulation");
		
		mRoot = new Group();
		Scene scene = new Scene(mRoot, 300, 300, Color.WHITE);
		setScene(scene);
		
		GridPane gridPane = new GridPane();
		gridPane.setPadding(new Insets(5));
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		
		Label mainLabel = new Label("Simulation parameters:");
		gridPane.add(mainLabel, 1, 0, 2, 1);
		
		Label durationLabel = new Label("Duration: ");
		gridPane.add(durationLabel, 0, 1);
		
		Label areaLabel = new Label("Area size: ");
		gridPane.add(areaLabel, 0, 2);
		
		Label usersCountLabel = new Label("Users count: ");
		gridPane.add(usersCountLabel, 0, 3);
		
		Label taxiesCountLabel = new Label("Taxies count: ");
		gridPane.add(taxiesCountLabel, 0, 4);
		
		Label taxiStationsCountLabel = new Label("Taxi stations count: ");
		gridPane.add(taxiStationsCountLabel, 0, 5);
		
		mDurationTextField = new TextField();
		mDurationTextField.setText(DURATION_DEFAULT);
		gridPane.add(mDurationTextField, 1, 1);
		
		mAreaSizeTextField = new TextField();
		mAreaSizeTextField.setText(AREA_SIZE_DEFAULT);
		gridPane.add(mAreaSizeTextField, 1, 2);
		
		mUsersCountTextField = new TextField();
		mUsersCountTextField.setText(USERS_COUNT_DEFAULT);
		gridPane.add(mUsersCountTextField, 1, 3);
		
		mTaxiesCountTextField = new TextField();
		mTaxiesCountTextField.setText(TAXIES_COUNT_DEFAULT);
		gridPane.add(mTaxiesCountTextField, 1, 4);
		
		mTaxiStationsCountTextField = new TextField();
		mTaxiStationsCountTextField.setText(TAXI_STATION_COUNT_DEFAULT);
		gridPane.add(mTaxiStationsCountTextField, 1, 5);
		
		Button login = new Button("Submit");
		login.setOnAction(new EventHandler<ActionEvent>() 
		{
			@Override
			public void handle(ActionEvent e)
			{
				close();
				mSubmitHandler.handle(getConfiguration());
			}
		});
		gridPane.add(login, 1, 6);
		GridPane.setHalignment(login, HPos.RIGHT);
		
		mRoot.getChildren().add(gridPane);
	}
	
	private SimulationConfiguration getConfiguration()
	{
		int duration = getValue(mDurationTextField.getText());
		int areaSize = getValue(mAreaSizeTextField.getText());
		int usersCount = getValue(mUsersCountTextField.getText());
		int taxiesCount = getValue(mTaxiesCountTextField.getText());
		int taxiStationsCount = getValue(mTaxiStationsCountTextField.getText());
		
		SimulationConfiguration config = new SimulationConfiguration(duration, 
				areaSize, usersCount, taxiesCount, taxiStationsCount);
		return config;
	}
	
	private int getValue(String text)
	{
		int value = Integer.parseInt(text);
		return (value > 0) ? value : 1;
	}
}
