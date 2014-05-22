package gui.charts.pieChart;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;

public class PieChartController extends Parent implements Initializable
{
	@FXML
	private PieChart pieChart;
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		System.out.println("PieChartController::initialize()");
	}
	
	public void setPieChartData(Map<String, Integer> data)
	{
		ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
		
		Iterator<Map.Entry<String, Integer>> iterator = data.entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<String, Integer> entry = iterator.next();
			pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
		}
		pieChart.setData(pieChartData);
	}
}
