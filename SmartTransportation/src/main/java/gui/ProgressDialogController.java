package gui;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;

public class ProgressDialogController extends Parent
{
	@FXML
	private ProgressIndicator progressIndicator;
	
    @FXML
    private void initialize() 
    {
    	System.out.println("ProgressDialogController::initialize()");
    }
    
    public void updateProgress(double progress)
    {
    	progressIndicator.setProgress(progress);
    }
}
