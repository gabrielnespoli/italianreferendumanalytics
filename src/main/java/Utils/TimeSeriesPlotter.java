package Utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class TimeSeriesPlotter extends JFrame {

    private ArrayList<double[]> xvaluesList;
    private ArrayList<double[]> yvaluesList;
    private ArrayList<String> labels;
    private String title;

    public TimeSeriesPlotter(String title, ArrayList<String> labels, ArrayList<double[]> xvaluesList, ArrayList<double[]> yvaluesList) {

        if (xvaluesList.size() != yvaluesList.size()) {
            throw new IllegalArgumentException("The lists labels, xvaluesArray and yvaluesArray have different sizes");
        }

        this.title = title;
        this.labels = labels;
        this.xvaluesList = xvaluesList;
        this.yvaluesList = yvaluesList;

        initUI();
    }

    private void initUI() {

        XYDataset dataset = createDataset();
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        add(chartPanel);

        pack();
        setTitle("Line chart");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /*
    The method will access the list of labels, xvalues and yvalues and will
    iterate through the lists and will create series with a label and values
    of each position of the list.
     */
    private XYDataset createDataset() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series;
        double[] xvalues;
        double[] yvalues;

        // iterate through the lists
        for (int i = 0; i < this.xvaluesList.size(); i++) {
            series = new XYSeries(this.labels.get(i));
            xvalues = this.xvaluesList.get(i);
            yvalues = this.yvaluesList.get(i);

            // iterate through the xvalues[] and yvalues[]
            for (int j = 0; j < xvalues.length; j++) {
                series.add(xvalues[j], yvalues[j]);
            }

            dataset.addSeries(series); // add the timeseries to the collection of timeseries
        }

        return dataset;
    }

    private JFreeChart createChart(XYDataset dataset) {

        JFreeChart chart = ChartFactory.createXYLineChart(
                this.title,
                "Time",
                "Frequency",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setSeriesPaint(2, Color.GREEN);
        renderer.setSeriesStroke(2, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        chart.getLegend().setFrame(BlockBorder.NONE);

        chart.setTitle(new TextTitle(this.title,
                new Font("Serif", java.awt.Font.BOLD, 16)));

        return chart;

    }

    public void plot() {
        SwingUtilities.invokeLater(() -> {
            this.setVisible(true);
        });
    }
}
