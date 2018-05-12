package utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Plotter {

    private ArrayList<double[]> xvaluesList;
    private ArrayList<double[]> yvaluesList;
    private ArrayList<String> labels;
    private String title;
    private List<String> allHistLabels;
    private List<double[]> allHistValues;
    private int bins;
    private String xaxis;
    private String yaxis;
    private JFreeChart chart;
    private ChartPanel chartPanel;

    public Plotter(String title, ArrayList<String> labels, ArrayList<double[]> xvaluesList, ArrayList<double[]> yvaluesList) {

        if (xvaluesList.size() != yvaluesList.size()) {
            throw new IllegalArgumentException("The lists labels, xvaluesArray and yvaluesArray have different sizes");
        }

        this.title = title;
        this.labels = labels;
        this.xvaluesList = xvaluesList;
        this.yvaluesList = yvaluesList;
        createChartTS();
    }

    public Plotter(String title, String xaxis, String yaxis, List<String> allHistLabels, List<double[]> histValues, int bins) {
        this.title = title;
        this.allHistLabels = allHistLabels;
        this.allHistValues = histValues;
        this.bins = bins;
        this.xaxis = xaxis;
        this.yaxis = yaxis;
        createChartHist();
    }

    public JFreeChart getChart() {
        return this.chart;
    }

    // initialize variables to plot the histogram
    private void createChartHist() {
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.FREQUENCY);

        for (int i = 0; i < this.allHistValues.size(); i++) {
            dataset.addSeries(this.allHistLabels.get(i), this.allHistValues.get(i), bins);
        }

        PlotOrientation orientation = PlotOrientation.VERTICAL;
        boolean show = true;
        boolean toolTips = false;
        boolean urls = false;
        this.chart = ChartFactory.createHistogram(this.title, this.xaxis, this.yaxis,
                dataset, orientation, show, toolTips, urls);

        // change the colors to translucid
        XYPlot plot = this.chart.getXYPlot();
        Paint[] paintArray = null;
        paintArray = new Paint[2];
        paintArray[0] = new Color(0x80ff0000, true);// translucent red, green & blue
        paintArray[1] = new Color(0x800000ff, true);

        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                paintArray,
                DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        createPanel();
    }

    // initialize variables to plot the timeseries
    private void createChartTS() {
        XYDataset dataset = createTSDataset();
        this.chart = buildTSChart(dataset);
        createPanel();
    }

    /*
    The method will access the list of labels, xvalues and yvalues and will
    iterate through the lists and will create series with a label and values
    of each position of the list.
     */
    private XYDataset createTSDataset() {
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

    private JFreeChart buildTSChart(XYDataset dataset) {

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

    private void createPanel() {
        this.chartPanel = new ChartPanel(this.chart);
        this.chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        this.chartPanel.setBackground(Color.white);
    }

    public void savePlot(String filename) {
        try {
            OutputStream out = new FileOutputStream(new File(filename));

            ChartUtilities.writeChartAsPNG(
                    out,
                    this.chart,
                    600,
                    400);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
