package gui;

import java.io.IOException;
import java.io.InputStream;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Window 
{
	protected Stage mStage;

	public void show()
	{
		if(mStage == null)
		{
			mStage = new Stage();
		}
	}
	
	public void maximize()
	{
		if((mStage != null) && mStage.isIconified())
		{
			mStage.setIconified(false);
		}
	}
	
	public void close()
	{
		if((mStage != null) && mStage.isShowing())
		{
			mStage.close();
		}
	}
	
	public boolean isShowing()
	{
		return ((mStage != null) && mStage.isShowing());
	}
	
	public boolean isIconified()
	{
		return ((mStage != null) && mStage.isIconified());
	}
	
	public ReadOnlyBooleanProperty showingProperty()
	{
		return mStage.showingProperty();
	}
	
	protected Object loadScene(String scenePath)
	{
		Parent rootGroup;
		FXMLLoader loader;		
		try 
		{
			loader = new FXMLLoader();
			loader.setBuilderFactory(new JavaFXBuilderFactory());
			loader.setLocation(getClass().getResource(scenePath));
			InputStream inputStream = getClass().getResourceAsStream(scenePath);
			rootGroup = (Parent)loader.load(inputStream);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
		
		Scene scene = new Scene(rootGroup);
		mStage.setScene(scene);
		mStage.show();
		return loader.getController();
	}
}
