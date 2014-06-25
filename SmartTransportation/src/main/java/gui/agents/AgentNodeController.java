package gui.agents;

import java.util.Iterator;
import java.util.List;

import uk.ac.imperial.presage2.util.location.Location;
import util.movement.Movement;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.util.Callback;

public class AgentNodeController 
{
	@FXML
	private Node container;
    @FXML
    private Label title;
    
    private Callback<Void, Void> mOnClickCallback;
    
    @FXML
    void initialize() 
    {
        assert container != null : "fx:id=\"container\" was not injected: check your FXML file.";
        assert title != null : "fx:id=\"title\" was not injected: check your FXML file.";
        
        container.setOnMouseClicked(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				if(mOnClickCallback != null)
				{
					mOnClickCallback.call(null);
				}
			}
		});
    }
    
    public void setTitle(String name)
    {
    	if(title != null)
    	{
    		title.setText(name);
    	}
    }
    
    public void setOnClickCallback(Callback<Void, Void> onClickCallback)
    {
    	mOnClickCallback = onClickCallback;
    }
    
    public Node getNode()
    {
    	return container;
    }
}
