package gui;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProgressDialog extends Stage
{
	private Group mRoot;
	private ProgressIndicator mIndicator;
	
	public ProgressDialog(Stage owner)
	{
		super();
		initOwner(owner);
		initLayout();
	}
	
	private void initLayout()
	{
		Modality modalityType = Modality.APPLICATION_MODAL;
		initModality(modalityType);
		setOpacity(1);
		setTitle("Processing Simulation");
		
		mRoot = new Group();
		Scene scene = new Scene(mRoot, 300, 300, Color.WHITE);
		setScene(scene);
		
		mIndicator = new ProgressIndicator();
		mIndicator.setProgress(-1);
		mIndicator.layoutXProperty().bind(scene.widthProperty().subtract(mIndicator.widthProperty()).divide(2));
		mIndicator.layoutYProperty().bind(scene.heightProperty().subtract(mIndicator.heightProperty()).divide(2));
		mRoot.getChildren().add(mIndicator);
	}
}
