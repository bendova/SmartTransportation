package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class MenuBarController 
{
	@FXML
	private Pane container;
    @FXML
    private Rectangle background;
    @FXML
    private ToggleButton toggle;
    @FXML
    private Slider timeLineSlider;
    
    @FXML
    private void initialize() 
    {
        assert background != null : "fx:id=\"background\" was not injected: check your FXML file 'MenuBar.fxml'.";
        assert toggle != null : "fx:id=\"toggle\" was not injected: check your FXML file 'MenuBar.fxml'.";
        
    }
    @FXML
    public void startSliderDrag()
    {
    	System.out.println("MenuBarController::startSliderDrag()");
    	
    	timeLineSlider.startFullDrag();
    }
    
    public Rectangle getBackground()
    {
    	return background;
    }
    public Pane getMenuBar()
    {
    	return container;
    }
    public ToggleButton getPlayPauseButton()
    {
    	return toggle;
    }
    public Slider getTimeLineSlider()
    {
    	return timeLineSlider;
    }
}
