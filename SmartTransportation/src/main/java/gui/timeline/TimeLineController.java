package gui.timeline;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class TimeLineController extends Parent implements Initializable
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
    private HBox controlsContainer;
    
    @Override
	public void initialize(URL arg0, ResourceBundle arg1) 
    {
		controlsContainer.translateXProperty().bind(background.widthProperty().
				subtract(controlsContainer.widthProperty()).divide(2));
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
