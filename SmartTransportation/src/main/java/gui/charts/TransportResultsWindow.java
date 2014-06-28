package gui.charts;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class TransportResultsWindow extends Stage
{
	public TransportResultsWindow(TreeMap<Number, Number> areaChartData)
	{
		assert(areaChartData != null);
		
		Scene scene = new Scene(createAreaChart(areaChartData, "Transport Results"));
		setScene(scene);
		centerOnScreen();
		setTitle("Transport Results");
	}
	
	public XYChart<Number, Number> createAreaChart(TreeMap<Number, Number> data, String title)
	{
		XYChart.Series<Number, Number> negativeSeries = new XYChart.Series<Number, Number>();
		XYChart.Series<Number, Number> pozitiveSeries = new XYChart.Series<Number, Number>();
		negativeSeries.setName("On time users");
		pozitiveSeries.setName("Late users");
		
		ObservableList<XYChart.Data<Number, Number>> negativeSeriesData = negativeSeries.getData();
		ObservableList<XYChart.Data<Number, Number>> pozitiveSeriesData = pozitiveSeries.getData();
		Iterator<Map.Entry<Number, Number>> iterator = data.entrySet().iterator();
		while(iterator.hasNext())
		{
			Map.Entry<Number, Number> entry = iterator.next();
			Number deltaTravelTime = entry.getKey();
			Number usersCount = entry.getValue();
			if(deltaTravelTime.intValue() <= 0)
			{
				negativeSeriesData.add(new XYChart.Data<Number, Number>(deltaTravelTime, usersCount));
			}
			else
			{
				pozitiveSeriesData.add(new XYChart.Data<Number, Number>(deltaTravelTime, usersCount));
			}
		}
		
		NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel("Delta arrival times");
		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Users count");
		XYChart<Number, Number> areaChart = new ScatterChart<Number, Number>(xAxis, yAxis);
		if(negativeSeries.getData().size() > 0)
		{
			areaChart.getData().add(negativeSeries);
		}
		if(pozitiveSeries.getData().size() > 0)
		{
			areaChart.getData().add(pozitiveSeries);
		}
		areaChart.setHorizontalZeroLineVisible(true);
		areaChart.setTitle(title);
		
		return areaChart;
	}
}
