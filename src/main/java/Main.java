
import analysis.CoocurrenceGraph;
import analysis.DistributionOverTime;
import analysis.GraphAnalysis;
import static analysis.GraphAnalysis.RESOURCES_LOCATION;
import analysis.TemporalAnalysis;
import index.IndexBuilder;
import index.IndexSearcher;
import io.ReadFile;
import static io.TxtUtils.txtToList;
import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.GraphReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;
import org.apache.lucene.document.Document;
import structure.MappedWeightedGraph;
import twitter4j.JSONException;

public class Main {

    public static int runner = (int) (Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) throws IOException, JSONException, ParseException, Exception {
        boolean plotDistrTweets = false;
        boolean createIndex = false;
        boolean clusterTopTerms = false;
        boolean generateCoocurrenceGraph = false;
        boolean extractKCoreCC = false;
        boolean useCache = true;
        boolean plotTS = false;
        boolean calculateTopAuthorities = true;
        boolean printAuthorities = true;
        boolean calculateKplayers = false;
        boolean printKplayers = false;
        double threshold = 0.07;

        String[] prefixYesNo = {"yes", "no"};

        if (plotDistrTweets) {
            List<double[]> histValues = new ArrayList<>();
            histValues.add(DistributionOverTime.loadHistogram("histogramYes.txt"));
            histValues.add(DistributionOverTime.loadHistogram("histogramNo.txt"));
            DistributionOverTime.plotHistogram(Arrays.asList(prefixYesNo), histValues, 20);
        }

        ArrayList<String> usersList;
        int[] nodes;
        Document[] docs;
        long id;
        int i;

        if (createIndex) {
            IndexBuilder.createIndexAllTweets();
            IndexBuilder.createYesNoIndex();
        }

        if (clusterTopTerms) {
            TemporalAnalysis.clusterTopNTerms(1000, 12, 20);

        }

        if (generateCoocurrenceGraph) {
            CoocurrenceGraph.generateCoocurrenceGraph();
        }

        if (extractKCoreCC) {
            GraphAnalysis.extractKCoreAndConnectedComponent(threshold);
        }

        String[] clusterTypes = {"kcore", "largestcc"};
        if (plotTS) {
            TemporalAnalysis.compareTimeSeriesOfTerms(3, prefixYesNo, clusterTypes);
        }

        int graphSize =  16815933;
        WeightedDirectedGraph g = new WeightedDirectedGraph(graphSize + 1);
        String graphFilename = "Official_SBN-ITA-2016-Net.gz";
        LongIntDict mapLong2Int = new LongIntDict();
        GraphReader.readGraphLong2IntRemap(g, RESOURCES_LOCATION + graphFilename, mapLong2Int, false);

        if (calculateTopAuthorities) {
            LinkedHashSet<Integer> users = GraphAnalysis.getUsersMentionedPolitician(useCache, mapLong2Int);
            // convert the set to array of int, needed by the method "SubGraph.extract"
            int[] usersIDs = new int[users.size()];
            i = 0;
            for (Integer userId : users) {
                usersIDs[i] = userId;
                i++;
            }
            
            MappedWeightedGraph gmap = GraphAnalysis.extractLargestCCofM(g, usersIDs, mapLong2Int);
            GraphAnalysis.saveTopKAuthorities(gmap, users, mapLong2Int, 1000, useCache);
        }

        if (printAuthorities) {
            GraphAnalysis.printSummaryAuthority(mapLong2Int.getInverted());
        }

        if (calculateKplayers) {
            String mGraphFilename = RESOURCES_LOCATION + "graph_largest_cc_of_M.gz";
            GZIPInputStream gzipIS = new GZIPInputStream(new FileInputStream(mGraphFilename));
            graphSize = (int) ReadFile.getLineCount(gzipIS);
            g = new WeightedDirectedGraph(graphSize + 1);
            GraphReader gr = new GraphReader(g, mGraphFilename, new CountDownLatch(runner));
            gr.run();
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
                GraphAnalysis.saveTopKPlayers(g, nodes, mapLong2Int, 500, 1);
            }
        }

        if (printKplayers) {
            GraphAnalysis.printTopKPlayers(mapLong2Int.getInverted());
        }
    }
}
