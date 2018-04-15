import Analysis.CoocurrenceGraph;
import Analysis.KcoreAndCC;
import Analysis.TemporalAnalysis;
import PreProcess.IndexBuilder;
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
        KcoreAndCC.extractKCoreAndConnectedComponent(threshold);
        
        String[] prefixYesNo = {"yes", "no"};
        String[] clusterTypes = {"kcore", "largestcc"};
        if (plot == true) {
            TemporalAnalysis.compareTimeSeriesOfTerms(3, prefixYesNo, clusterTypes);
        }
        
    }
}
