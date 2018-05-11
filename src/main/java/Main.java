
import analysis.GraphAnalysis;
import static analysis.GraphAnalysis.RESOURCES_LOCATION;
import static analysis.GraphAnalysis.runner;
import analysis.TemporalAnalysis;
import index.IndexBuilder;
import index.IndexSearcher;
import static io.TxtUtils.txtToList;
import it.stilo.g.algo.KppNeg;
import it.stilo.g.structures.DoubleValues;
import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.GraphReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.document.Document;
import twitter4j.JSONException;

public class Main {

    public static void main(String[] args) throws IOException, JSONException, ParseException, Exception {
        boolean createIndex = false;
        boolean useCache = true;
        boolean plotTS = false;
        boolean calculateTopAuthorities = true;
        boolean printAuthorities = true;
        boolean calculateKplayers = false;
        boolean printKplayers = false;
        double threshold = 0.07;

        ArrayList<String> usersList;
        int[] nodes;
        Document[] docs;
        long id;
        int i;

        if (createIndex) {
            IndexBuilder.createIndexAllTweets();
            IndexBuilder.createYesNoIndex();
        }

        /*
        
        TemporalAnalysis.clusterTopNTerms(1000, 12, 20);
        CoocurrenceGraph.generateCoocurrenceGraph();
        GraphAnalysis.extractKCoreAndConnectedComponent(threshold);
         */
        String[] prefixYesNo = {"yes", "no"};
        String[] clusterTypes = {"kcore", "largestcc"};
        if (plotTS) {
            TemporalAnalysis.compareTimeSeriesOfTerms(3, prefixYesNo, clusterTypes);
        }
        int graphSize = 100000; //16815933;
        WeightedDirectedGraph g = new WeightedDirectedGraph(graphSize + 1);
        String graphFilename = "data.gz"; //"Official_SBN-ITA-2016-Net.gz";
        LongIntDict mapLong2Int = new LongIntDict();
        GraphReader.readGraphLong2IntRemap(g, RESOURCES_LOCATION + graphFilename, mapLong2Int, false);

        if (calculateTopAuthorities) {
            GraphAnalysis.saveTopKAuthorities(g, mapLong2Int, 1000, useCache);
        }

        if (printAuthorities) {
            GraphAnalysis.printSummaryAuthority(mapLong2Int.getInverted());
        }

        if (calculateKplayers) {
            for (String supportType : prefixYesNo) {
                usersList = txtToList(RESOURCES_LOCATION + supportType + "_M.txt", String.class); // retrieve the users names
                nodes = new int[usersList.size()];
                i = 0;
                // from the users names to their Twitter ID, then to their respective position in the graph (int)
                // name -> twitterID -> position in the graph
                for (String username : usersList) {
                    docs = IndexSearcher.searchByField("all_tweets_index/", "user", username, 1);

                    if (docs != null) {
                        id = Long.parseLong(docs[0].get("id"));  //read just the first resulting doc
                        nodes[i] = mapLong2Int.get(id);  // retrieve the twitter ID (long) and covert to int (the position in the graph)
                    }
                    i++;
                }
                GraphAnalysis.saveTopKPlayers(g, Arrays.copyOfRange(nodes, 0, 100), mapLong2Int, 500, 200);
            }
        }

        if (printKplayers) {
            GraphAnalysis.printTopKPlayers(mapLong2Int.getInverted());
        }
    }
}
