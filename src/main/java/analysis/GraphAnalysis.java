package analysis;

import com.google.common.primitives.Ints;
import io.ReadFile;
import io.TxtUtils;
import com.google.common.util.concurrent.AtomicDouble;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.TIntLongMap;
import index.IndexSearcher;
import static io.TxtUtils.txtToList;
import it.stilo.g.algo.ConnectedComponents;
import it.stilo.g.algo.CoreDecomposition;
import it.stilo.g.algo.GraphInfo;
import it.stilo.g.algo.HubnessAuthority;
import it.stilo.g.algo.KppNeg;
import it.stilo.g.algo.SubGraphByEdgesWeight;
import it.stilo.g.structures.Core;
import it.stilo.g.util.NodesMapper;
import java.io.IOException;
import java.util.List;
import it.stilo.g.algo.SubGraph;
import it.stilo.g.algo.UnionDisjoint;
import it.stilo.g.structures.DoubleValues;
import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.structures.WeightedGraph;
import it.stilo.g.structures.WeightedUndirectedGraph;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Integer.min;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import structure.MappedWeightedGraph;
import structure.ReadTxtException;
import utils.GraphUtils;

public abstract class GraphAnalysis {

    public static String RESOURCES_LOCATION = "src/main/resources/";
    public static int runner = (int) (Runtime.getRuntime().availableProcessors());

    private static int getNumberClusters(String graph) throws IOException {
        //String graph = "src/main/resources/yes_graph.txt";
        ReadFile rf = new ReadFile();
        String[] lines = rf.readLines(graph);

        Set<Double> clusters = new HashSet<>();
        int n = lines.length;
        for (int i = 0; i < n; i++) {
            String[] line = lines[i].split(" ");
            Double c = Double.parseDouble(line[3]);
            clusters.add(c);
        }
        return clusters.size();
    }

    private static List<Integer> getNumberNodes(String graph, int c) throws IOException {
        ReadFile rf = new ReadFile();
        String[] lines = rf.readLines(graph);
        List<Integer> numberNodes = new ArrayList<>();
        int n = lines.length;
        for (int cIter = 0; cIter < c; cIter++) {
            Set<String> words = new HashSet<String>();
            for (int i = 0; i < n; i++) {
                String[] line = lines[i].split(" ");
                int cc = Integer.parseInt(line[3]);
                if (cc == cIter) {
                    words.add(line[0]);
                    words.add(line[1]);
                }
            }
            numberNodes.add(words.size());
        }
        return numberNodes;
    }

    private static WeightedUndirectedGraph addNodesGraph(WeightedUndirectedGraph g, int k, String graph, NodesMapper<String> mapper) throws IOException {
        // add the nodes from the a file created with coocurrencegraph.java, and returns the graph
        ReadFile rf = new ReadFile();
        String[] lines = rf.readLines(graph);

        // map the words into id for g stilo
        //NodesMapper<String> mapper = new NodesMapper<String>();
        // creathe the graph
        // keep in mind that the id of a word is mapper.getId(s1) - 1 (important the -1)
        int n = lines.length;
        for (int i = 0; i < n; i++) {
            // split the line in 3 parts: node1, node2, and weight
            String[] line = lines[i].split(" ");
            if (Integer.parseInt(line[3]) == k) {
                String node1 = line[0];
                String node2 = line[1];
                Double w = Double.parseDouble(line[2]);
                // the graph is directed, add links in both ways
                g.add(mapper.getId(node1) - 1, mapper.getId(node2) - 1, w);
                //g.add(mapper.getId(node2) - 1, mapper.getId(node1) - 1, w);
            }

        }
        return g;
    }

    private static WeightedUndirectedGraph normalizeGraph(WeightedUndirectedGraph g) {
        // normalize the weights of the edges
        // Normalize the weights
        double suma = 0;
        // go in each node

        for (int i = 0; i < g.size - 1; i++) {
            suma = 0;

            // calculate the sum of the weights of this node with the neighbours
            for (int j = 0; j < g.weights[i].length; j++) {
                suma = suma + g.weights[i][j];
            }

            // update the weights by dividing by the total weight sum
            for (int j = 0; j < g.weights[i].length; j++) {
                g.weights[i][j] = g.weights[i][j] / suma;
            }

        }

        return g;
    }

    private static WeightedUndirectedGraph kcore(WeightedUndirectedGraph g) throws InterruptedException {
        // calculates the kcore and returns a graph. Now its not working who knows why
        WeightedUndirectedGraph g1 = UnionDisjoint.copy(g, runner);
        Core cc = CoreDecomposition.getInnerMostCore(g1, runner);
        System.out.println("Kcore");
        System.out.println("Minimum degree: " + cc.minDegree);
        System.out.println("Vertices: " + cc.seq.length);
        System.out.println("Seq: " + cc.seq);

        g1 = UnionDisjoint.copy(g, 2);
        WeightedUndirectedGraph s = SubGraph.extract(g1, cc.seq, runner);
        return s;
    }

    private static Set<Integer> getMaxSet(Set<Set<Integer>> comps) {
        int m = 0;
        Set<Integer> max_set = null;
        // get largest component
        for (Set<Integer> innerSet : comps) {
            if (innerSet.size() > m) {
                max_set = innerSet;
                m = innerSet.size();
            }
        }
        return max_set;
    }

    private static WeightedUndirectedGraph getLargestCC(WeightedUndirectedGraph g) throws InterruptedException {
        // this get the largest component of the graph and returns a graph too
        //System.out.println(Arrays.deepToString(g.weights));
        int[] all = new int[g.size];
        for (int i = 0; i < g.size; i++) {
            all[i] = i;
        }
        System.out.println("CC");
        Set<Set<Integer>> comps = ConnectedComponents.rootedConnectedComponents(g, all, runner);

        Set<Integer> max_set = getMaxSet(comps);
        int[] subnodes = new int[max_set.size()];
        Iterator<Integer> iterator = max_set.iterator();
        for (int j = 0; j < subnodes.length; j++) {
            subnodes[j] = iterator.next();
        }

        WeightedUndirectedGraph s = SubGraph.extract(g, subnodes, runner);
        return s;
    }

    /* 
    iterate through all the edges, recovering the terms.
    'edges' is a matrix, in which each row is a termID1, and in each column is 
    another termID2 that has an edge with termID1. 
    Ex:
    [0] = [1, 5, 6]
    [1] = [0, 8]
    ...
    Map back each termID to the term string and save to the edges in the following
    format:
    term1 term2 clusterID
     */
    private static void saveGraphToFile(PrintWriter pw, NodesMapper<String> mapper, int[][] edges, int clusterID) throws IOException {
        String term1 = "", term2 = "";

        for (int i = 0; i < edges.length; i++) {
            if (edges[i] != null) {
                term1 = mapper.getNode(i + 1);
                for (int j = 0; j < edges[i].length; j++) {
                    term2 = mapper.getNode(edges[i][j] + 1);
                    pw.println(term1 + " " + term2 + " " + clusterID);
                }
            }
        }
    }

    public static void extractKCoreAndConnectedComponent(double threshold) throws IOException, ParseException, Exception {

        // do the same analysis for the yes-group and no-group
        String[] prefixYesNo = {"yes", "no"};
        for (String prefix : prefixYesNo) {

            // Get the number of clusters
            int c = getNumberClusters(RESOURCES_LOCATION + prefix + "_graph.txt");

            // Get the number of nodes inside each cluster
            List<Integer> numberNodes = getNumberNodes(RESOURCES_LOCATION + prefix + "_graph.txt", c);

            PrintWriter pw_cc = new PrintWriter(new FileWriter(RESOURCES_LOCATION + prefix + "_largestcc.txt")); //open the file where the largest connected component will be written to
            PrintWriter pw_kcore = new PrintWriter(new FileWriter(RESOURCES_LOCATION + prefix + "_kcore.txt")); //open the file where the kcore will be written to

            // create the array of graphs
            WeightedUndirectedGraph[] gArray = new WeightedUndirectedGraph[c];
            for (int i = 0; i < c; i++) {
                System.out.println();
                System.out.println("Cluster " + i);

                gArray[i] = new WeightedUndirectedGraph(numberNodes.get(i) + 1);

                // Put the nodes,
                NodesMapper<String> mapper = new NodesMapper<String>();
                gArray[i] = addNodesGraph(gArray[i], i, RESOURCES_LOCATION + prefix + "_graph.txt", mapper);

                //normalize the weights
                gArray[i] = normalizeGraph(gArray[i]);

                AtomicDouble[] info = GraphInfo.getGraphInfo(gArray[i], 1);
                System.out.println("Nodes:" + info[0]);
                System.out.println("Edges:" + info[1]);
                System.out.println("Density:" + info[2]);

                // extract remove the edges with w<t
                gArray[i] = SubGraphByEdgesWeight.extract(gArray[i], threshold, 1);

                // get the largest CC and save to a file
                WeightedUndirectedGraph largestCC = getLargestCC(gArray[i]);
                saveGraphToFile(pw_cc, mapper, largestCC.in, i);

                // Get the inner core and save to a file
                WeightedUndirectedGraph kcore = kcore(gArray[i]);
                saveGraphToFile(pw_kcore, mapper, kcore.in, i);
            }

            pw_cc.close();
            pw_kcore.close();
        }
    }

    /* 
    Iterate through the file of kcore/CC identifying each cluster and 
    storing it as a hashmap that the keys are the cluster IDs and the 
    values are the sets.
    Ex:
        "0": {"hi", "hello"}
        "1": {"Brazil", "Spain"}
     */
    public static HashMap<Integer, LinkedHashSet<String>> loadClusters(String clusterDirectory) throws IOException {
        FileInputStream is = new FileInputStream(clusterDirectory);
        InputStreamReader ir = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(ir);
        String line;
        String[] lineSplit;
        String term;
        Integer clusterID;
        LinkedHashSet<String> cluster;
        HashMap<Integer, LinkedHashSet<String>> hmClusterIDTerms = new HashMap<>();
        while ((line = br.readLine()) != null) {
            lineSplit = line.split(" ");
            term = lineSplit[0];
            clusterID = Integer.parseInt(lineSplit[2]);

            //update the cluster 'clusterID' adding 'term'. Instantiate the cluster if it's empty
            if ((cluster = hmClusterIDTerms.get(clusterID)) == null) {
                cluster = new LinkedHashSet<>();
            }
            cluster.add(term);
            hmClusterIDTerms.put(clusterID, cluster);
        }

        return hmClusterIDTerms;
    }

    public static LinkedHashSet<Integer> getUsersMentionedPolitician(boolean useCache, LongIntDict mapLong2Int) throws ReadTxtException, ParseException, IOException {
        HashMap<String, LinkedHashSet<Integer>> hmGroupType2Users = new HashMap<>(); //data structure that will keep the two sets of users (for yes and no group)
        List<String> userMentionPolitician;
        double counter;
        String username;
        long id;
        Document[] docs;

        if (!useCache) {
            // iterate through the yes and no group extracting all the users that mentioned
            // a politician
            String[] typeGroups = {"yes", "no"};
            for (String typeGroup : typeGroups) {
                LinkedHashSet<Integer> usersUnique = new LinkedHashSet<>();

                // retrieve the twitter id of the users, instead of the name, given that
                // the graph uses the ID
                userMentionPolitician = TxtUtils.txtToList(RESOURCES_LOCATION + typeGroup + "_users_mention_politicians.txt");

                // retrieve all the unique users that mentioned the politicians
                counter = 0.0;

                for (String row : userMentionPolitician) {
                    System.out.println("Calculating authorities: " + counter / userMentionPolitician.size() * 100.0 + " % Done");
                    counter += 1.0;

                    username = row.split(" ")[0];  // get the user screen name
                    docs = IndexSearcher.searchByField("all_tweets_index/", "user", username, 1);

                    if (docs != null) {
                        id = Long.parseLong(docs[0].get("id"));  //read just the first resulting doc
                        usersUnique.add(mapLong2Int.get(id));  // retrieve the twitter ID (long) and covert to int
                    }
                }
                hmGroupType2Users.put(typeGroup, usersUnique);
            }
            // save the set of users
            TxtUtils.iterableToTxt(RESOURCES_LOCATION + "no_unique_users_mention_politician.txt", hmGroupType2Users.get("no"));
            TxtUtils.iterableToTxt(RESOURCES_LOCATION + "yes_unique_users_mention_politician.txt", hmGroupType2Users.get("yes"));

        } else {
            try {
                hmGroupType2Users.put("no", new LinkedHashSet(txtToList(RESOURCES_LOCATION + "no_unique_users_mention_politician.txt", Integer.class)));
                hmGroupType2Users.put("yes", new LinkedHashSet(txtToList(RESOURCES_LOCATION + "yes_unique_users_mention_politician.txt", Integer.class)));
            } catch (Exception ex) {
                throw new ReadTxtException();
            }
        }

        // creates a unique set with all the unique users
        LinkedHashSet<Integer> users = new LinkedHashSet<>(hmGroupType2Users.get("no"));
        users.addAll(hmGroupType2Users.get("yes"));

        return users;
    }

    public static MappedWeightedGraph extractLargestCCofM(WeightedDirectedGraph g, int[] usersIDs, LongIntDict mapLong2Int) throws InterruptedException, IOException {
        // extract the subgraph induced by the users that mentioned the politicians
        System.out.println("Extracting the subgraph induced by M");
        g = SubGraph.extract(g, usersIDs, runner);

        // The SubGraph.extract() creates a graph of the same size as the old graph
        // and it raises an exception due to insufficient memory.
        // We had to resize the graph.   
        LongIntDict dictResize = new LongIntDict();
        g = GraphUtils.resizeGraph(g, dictResize, usersIDs.length);
        // map the id of the old big graph to the new ones
        usersIDs = new int[usersIDs.length];
        int i = 0;
        TLongIntIterator iterator = dictResize.getIterator();
        while (iterator.hasNext()) {
            iterator.advance();
            usersIDs[i] = iterator.value();
            i++;
        }

        // extract the largest connected component
        System.out.println("Extracting the largest connected component of the subgraph induced by M");
        Set<Integer> setMaxCC = getMaxSet(ConnectedComponents.rootedConnectedComponents(g, usersIDs, runner));
        g = SubGraph.extract(g, Ints.toArray(setMaxCC), runner);

        // save the largest CC of M
        System.out.println("Saving the graph");
        TIntLongMap revDictResize = dictResize.getInverted();
        GraphUtils.saveDirectGraph2Mappings(g, RESOURCES_LOCATION + "graph_largest_cc_of_M.gz", revDictResize, mapLong2Int.getInverted());

        return new MappedWeightedGraph(g, revDictResize);
    }

    public static void saveTopKAuthorities(MappedWeightedGraph gmap, LinkedHashSet<Integer> users, LongIntDict mapLong2Int, int topk, boolean useCache) throws InterruptedException, IOException {
        WeightedGraph g = gmap.getWeightedGraph();
        TIntLongMap mapInt2Long = gmap.getMap();

        // get the authorities
        ArrayList<ArrayList<DoubleValues>> authorities = HubnessAuthority.compute(g, 0.00001, runner);
        ArrayList<DoubleValues> scores = authorities.get(0);

        // map back the ids in 'score' to the previous id, before the resizing, then map back to the twitter ID
        ArrayList<DoubleValues> scoreMappedID = new ArrayList<>();
        for (DoubleValues score : scores) {
            scoreMappedID.add(new DoubleValues((int) mapInt2Long.get(score.index), score.value));
        }

        scores = scoreMappedID;

        // save the first topk authorities
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "top_authorities.txt", scores.subList(0, min(topk, scores.size())));
    }

    public static void printAuthorities(TIntLongMap mapIntToLong) throws IOException, ParseException, InterruptedException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        ArrayList<Integer> yesUniqueUsersMentionPolitician = txtToList(RESOURCES_LOCATION + "yes_unique_users_mention_politician.txt", Integer.class);
        ArrayList<Integer> noUniqueUsersMentionPolitician = txtToList(RESOURCES_LOCATION + "no_unique_users_mention_politician.txt", Integer.class);
        ArrayList<ArrayList> topAuthorities = txtToList(RESOURCES_LOCATION + "top_authorities.txt", Integer.class, Double.class);

        // initialize the summaryAuthority
        LinkedHashMap<String, ArrayList> summaryAuthority = new LinkedHashMap<>();
        summaryAuthority.put("yes", new ArrayList());
        summaryAuthority.put("yesunique", new ArrayList());
        summaryAuthority.put("no", new ArrayList());
        summaryAuthority.put("nounique", new ArrayList());

        for (ArrayList values : topAuthorities) {
            // users that mentioned the yes supporters
            if (yesUniqueUsersMentionPolitician.contains(values.get(0))) {
                // summaryAuthority.put("yes",  summaryAuthority.get("yes").add(values.get(0)));
                summaryAuthority.get("yes").add(values.get(0));
                // users that mentioned the yes supporters but not the no supporters
                if (!noUniqueUsersMentionPolitician.contains(values.get(0))) {
                    //summaryAuthority.put("yesunique", summaryAuthority.get("yesunique") + 1);
                    summaryAuthority.get("yesunique").add(values.get(0));
                }
            }
            // users that mentioned the no supporters
            if (noUniqueUsersMentionPolitician.contains(values.get(0))) {
                //summaryAuthority.put("no", summaryAuthority.get("no") + 1);
                summaryAuthority.get("no").add(values.get(0));

                // users that mentioned the no supporters but not the yes supporters
                if (!yesUniqueUsersMentionPolitician.contains(values.get(0))) {
                    //summaryAuthority.put("nounique", summaryAuthority.get("nounique") + 1);
                    summaryAuthority.get("nounique").add(values.get(0));
                }

            }
        }

        System.out.println(summaryAuthority.get("yes").size() + " of the top authorities mentioned the Yes supporters");
        System.out.println(summaryAuthority.get("no").size() + " of the top authorities mentioned the No supporters");
        System.out.println(summaryAuthority.get("yesunique").size() + " of the top authorities mentioned just the Yes supporters");
        System.out.println(summaryAuthority.get("nounique").size() + " of the top authorities mentioned just the No supporters");
        System.out.println();

        Document[] docs;
        Long twitterID;
        // print the authorities that mentioned uniquely the Yes supporters
        System.out.println("---------Top authorities that mentioned just the Yes supporters---------");
        for (Object userID : summaryAuthority.get("yesunique")) {
            twitterID = mapIntToLong.get((int) userID);
            docs = IndexSearcher.searchByField("all_tweets_index/", "id", twitterID, 1);

            if (docs != null) {
                System.out.println(docs[0].get("user"));
            }
        }

        // print the authorities that mentioned uniquely the No supporters
        System.out.println("---------Top authorities that mentioned just the No supporters---------");
        for (Object userID : summaryAuthority.get("nounique")) {
            twitterID = mapIntToLong.get((int) userID);
            docs = IndexSearcher.searchByField("all_tweets_index/", "id", twitterID, 1);

            if (docs != null) {
                System.out.println(docs[0].get("user"));
            }
        }
    }

    /*
    The method receives a graph, extract the nodes that have a degree higher than threshold,
    and finally calculate their reachability using the KppNeg algorithm. The top 500 
    players are saved in disk
     */
    public static List<ImmutablePair> getTopKPlayers(WeightedDirectedGraph g, int[] nodes, LongIntDict mapLong2Int, int topk, int threshold) throws InterruptedException, IOException, ParseException {
        ArrayList<Integer> subGraphNodes = new ArrayList<>();

        // extract just the graph induced by nodes.
        g = SubGraph.extract(g, nodes, runner);

        // iterate through the graph and add to a list just the nodes with high degree
        for (int i = 1; i < g.out.length; i++) {
            if ((g.out[i] != null && g.out[i].length >= threshold) || (g.in[i] != null && g.in[i].length >= threshold)) {
                subGraphNodes.add(i);
            }
        }

        g = SubGraph.extract(g, subGraphNodes.stream().mapToInt(i -> i).toArray(), runner);

        long start = System.currentTimeMillis();
        List<DoubleValues> brokers = KppNeg.searchBroker(g, g.getVertex(), runner); //subGraphNodes.stream().mapToInt(i -> i).toArray()
        long end = System.currentTimeMillis();
        System.out.println("Took : " + ((end - start) / 1000) + " seconds");

        // convert from the node position of the graph into the TwitterID, and then
        // to the Twitter name
        List<ImmutablePair> brokersUsername = new ArrayList<>();
        TIntLongMap mapIntToLong = mapLong2Int.getInverted();
        long twitterID;
        Document[] docs;
        String username;
        for (DoubleValues broker : brokers) {
            twitterID = mapIntToLong.get((int) broker.index);
            docs = IndexSearcher.searchByField("all_tweets_index/", "id", twitterID, 1);

            username = null;
            if (docs != null) {
                username = docs[0].get("user");
                brokersUsername.add(new ImmutablePair<String, Double>(username, broker.value));
            } else {
                docs = IndexSearcher.searchByField("all_tweets_index/", "rt_id", twitterID, 1);
                if (docs != null) {
                    username = docs[0].get("rt_user");
                    brokersUsername.add(new ImmutablePair<String, Double>(username, broker.value));
                }

            }
        }

        return brokersUsername.subList(0, min(topk, brokersUsername.size()));
    }

}
