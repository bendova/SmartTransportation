package gui.screens;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;

public class LoadingScreen extends Parent
{
	@FXML
	private ProgressIndicator progressIndicator;
	
    @FXML
    private void initialize() 
    {
    	System.out.println("LoadingScreen::initialize()");
    }
    
    public void updateProgress(double progress)
    {
    	progressIndicator.setProgress(progress);
    }
}
