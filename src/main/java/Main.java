import analysis.CoocurrenceGraph;
import analysis.GraphAnalysis;
import analysis.TemporalAnalysis;
import index.IndexBuilder;
import it.stilo.g.structures.WeightedUndirectedGraph;
import java.io.IOException;
import java.text.ParseException;
import twitter4j.JSONException;

public class Main {
    public static void main(String []args) throws IOException, JSONException, ParseException, Exception {
        boolean useCache = true;
        boolean plot = true;
        double threshold = 0.07;
        
        if (!useCache) {
            IndexBuilder.createIndexAllTweets();
            IndexBuilder.createYesNoIndex();
        }
        
        TemporalAnalysis.clusterTopNTerms(1000, 12, 20);
        CoocurrenceGraph.generateCoocurrenceGraph();
        GraphAnalysis.extractKCoreAndConnectedComponent(threshold);
        
        String[] prefixYesNo = {"yes", "no"};
        String[] clusterTypes = {"kcore", "largestcc"};
        if (plot == true) {
            TemporalAnalysis.compareTimeSeriesOfTerms(3, prefixYesNo, clusterTypes);
        }
        
        // do the authorities and hubs analyses
        int graphSize = 16815933;
        String graphFilename = "Official_SBN-ITA-2016-Net";
        WeightedUndirectedGraph g = GraphAnalysis.readGraph(graphSize, graphFilename);
        
    }
}