package gui.screens;

import javafx.application.Platform;
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

	public void updateProgress(final double progress)
	{
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				progressIndicator.setProgress(progress);
			}
		});
	}
}
