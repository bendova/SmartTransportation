package gui.agents;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class AgentNodeController 
{
	@FXML
	private Node container;
    @FXML
    private Label title;
    @FXML
    private Node shape;
    
    @FXML
    void initialize() 
    {
        assert container != null : "fx:id=\"container\" was not injected: check your FXML file.";
        assert title != null : "fx:id=\"title\" was not injected: check your FXML file.";
    }
    
    public void setTitle(String name)
    {
    	if(title != null)
    	{
    		title.setText(name);
    	}
    }
    
    public Node getNode()
    {
    	return container;
    }
}
