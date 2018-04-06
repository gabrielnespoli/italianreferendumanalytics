/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Analysis;

import IO.ReadFile;
import com.google.common.util.concurrent.AtomicDouble;
import it.stilo.g.algo.ConnectedComponents;
import it.stilo.g.algo.CoreDecomposition;
import it.stilo.g.algo.GraphInfo;
import it.stilo.g.algo.SubGraphByEdgesWeight;
import it.stilo.g.structures.Core;
import it.stilo.g.util.NodesMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import it.stilo.g.algo.SubGraph;
import it.stilo.g.algo.UnionDisjoint;
import it.stilo.g.structures.WeightedUndirectedGraph;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.lucene.queryparser.classic.ParseException;

public class KcoreAndCC {

    public static final String RESOURCES_LOCATION = "src/main/resources/";

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
        //String graph = "src/main/resources/yes_graph.txt";
        ReadFile rf = new ReadFile();
        String[] lines = rf.readLines(graph);
        //WeightedUndirectedGraph[] gArray = new WeightedUndirectedGraph[c];
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
        //String graph = "src/main/resources/yes_graph.txt";
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
        WeightedUndirectedGraph g1 = UnionDisjoint.copy(g, 2);
        Core cc = CoreDecomposition.getInnerMostCore(g1, 1);
        System.out.println("Kcore");
        System.out.println("Minimum degree: " + cc.minDegree);
        System.out.println("Vertices: " + cc.seq.length);
        System.out.println("Seq: " + cc.seq);

        g1 = UnionDisjoint.copy(g, 2);
        WeightedUndirectedGraph s = SubGraph.extract(g1, cc.seq, 1);
        return s;
    }

    private static WeightedUndirectedGraph getLargestCC(WeightedUndirectedGraph g) throws InterruptedException {
        // this get the largest component of the graph and returns a graph too
        System.out.println(Arrays.deepToString(g.weights));
        int[] all = new int[g.size];
        for (int i = 0; i < g.size; i++) {
            all[i] = i;
        }
        System.out.println("CC");
        Set<Set<Integer>> comps = ConnectedComponents.rootedConnectedComponents(g, all, 2);
        //System.out.println(comps.size());

        int counter = 0;
        int m = 0;
        Set<Integer> max_set = null;
        // get largest component
        for (Set<Integer> innerSet : comps) {
            if (innerSet.size() > m) {
                max_set = innerSet;
                m = innerSet.size();
            }
            counter++;
        }

        int[] subnodes = new int[max_set.size()];
        Iterator<Integer> iterator = max_set.iterator();
        for (int j = 0; j < subnodes.length; j++) {
            subnodes[j] = iterator.next();
        }

        WeightedUndirectedGraph s = SubGraph.extract(g, subnodes, 1);
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
            WeightedUndirectedGraph[] gCC = new WeightedUndirectedGraph[c];
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
}
