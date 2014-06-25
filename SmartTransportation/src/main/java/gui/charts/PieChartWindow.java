package gui.charts;

import java.util.Iterator;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.stage.Stage;

public class PieChartWindow extends Stage
{
	public PieChartWindow(Map<String, Integer> chartData, String title)
	{
		assert(chartData != null);
		assert(title != null);

		Scene scene = new Scene(createPieChart(chartData, title));
		setScene(scene);
		centerOnScreen();
	}
	
	private PieChart createPieChart(Map<String, Integer> chartData, String title)
	{
		PieChart pieChart = new PieChart();
		
		ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
		
		Iterator<Map.Entry<String, Integer>> iterator = chartData.entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<String, Integer> entry = iterator.next();
			pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
		}
		pieChart.setData(pieChartData);
		pieChart.setTitle(title);
		
		return pieChart;
	}
}
