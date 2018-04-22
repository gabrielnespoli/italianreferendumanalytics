
import analysis.GraphAnalysis;
import static analysis.GraphAnalysis.RESOURCES_LOCATION;
import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedUndirectedGraph;
import it.stilo.g.util.GraphReader;
import java.io.IOException;
import java.text.ParseException;
import twitter4j.JSONException;

public class Main {

    public static void main(String[] args) throws IOException, JSONException, ParseException, Exception {
        boolean useCache = true;
        boolean plot = true;
        boolean calculateTopAuthorities = false;
        boolean printAuthorities = true;
        double threshold = 0.07;
        /*
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
         */

        // do the authorities and hubs analyses
        int graphSize = 16815933;
        WeightedUndirectedGraph g = new WeightedUndirectedGraph(graphSize + 1);
        String graphFilename = "data.gz"; //"Official_SBN-ITA-2016-Net.gz";
        LongIntDict mapLong2Int = new LongIntDict();
        GraphReader.readGraphLong2IntRemap(g, RESOURCES_LOCATION + graphFilename, mapLong2Int, false);

        if (calculateTopAuthorities) {
            GraphAnalysis.saveTopKAuthorities(g, mapLong2Int, 1000, useCache);
        }

        if (printAuthorities) {
            GraphAnalysis.printSummaryAuthority(mapLong2Int.getInverted());
        }
    }
}
