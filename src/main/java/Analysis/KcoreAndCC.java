/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Analysis;

import com.google.common.util.concurrent.AtomicDouble;
import gnu.trove.map.TIntLongMap;
import it.stilo.g.algo.ConnectedComponents;
import it.stilo.g.algo.CoreDecomposition;
import it.stilo.g.algo.GraphInfo;
import it.stilo.g.algo.SubGraphByEdgesWeight;
import it.stilo.g.structures.Core;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import it.stilo.g.algo.SubGraph;
import it.stilo.g.structures.WeightedUndirectedGraph;
import it.stilo.g.util.GraphWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.queryparser.classic.ParseException;

public class KcoreAndCC {

    public static int getNumberClusters(String graph) throws IOException {
        //String graph = "src/main/resources/yes_graph.txt";
        ReadFile rf = new ReadFile();
        String[] lines = rf.readLines(graph);

        Set<Double> clusters = new HashSet<Double>();
        int n = lines.length;
        for (int i = 0; i < n; i++) {
            String[] line = lines[i].split(" ");
            Double c = Double.parseDouble(line[3]);
            clusters.add(c);
        }
        return clusters.size();
    }

    public static List<Integer> getNumberNodes(String graph, int c) throws IOException {
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

    public static WeightedUndirectedGraph addNodesGraph(WeightedUndirectedGraph g, int k, String graph, NodesMapper<String> mapper) throws IOException {
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

    public static WeightedUndirectedGraph normalizeGraph(WeightedUndirectedGraph g) {
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

    public static WeightedUndirectedGraph kcore(WeightedUndirectedGraph g) throws InterruptedException {
        // calculates the kcore and returns a graph. Now its not working who knows why
        Core cc = CoreDecomposition.getInnerMostCore(g, 1);
        System.out.println("Kcore");
        System.out.println("Minimum degree: " + cc.minDegree);
        System.out.println("Vertices: " + cc.seq.length);
        System.out.println("Seq: " + cc.seq);


        WeightedUndirectedGraph s = SubGraph.extract(g, cc.seq, 1);
        return s;
    }

    public static WeightedUndirectedGraph getLargestCC(WeightedUndirectedGraph g) throws InterruptedException {
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

    public static void main(String[] args) throws IOException, ParseException, Exception {

        // when the nodes have an edge less than this, remove the edge
        double t = 0.1;

        // Get the number of clusters
        int c = getNumberClusters("src/main/resources/yes_graph.txt");

        // Get the number of nodes inside each cluster
        List<Integer> numberNodes = getNumberNodes("src/main/resources/yes_graph.txt", c);

        // create the array of graphs
        WeightedUndirectedGraph[] gArray = new WeightedUndirectedGraph[c];
        WeightedUndirectedGraph[] gCC = new WeightedUndirectedGraph[c];
        for (int i = 0; i < 1; i++) {
            System.out.println();
            System.out.println("Cluster " + i);

            gArray[i] = new WeightedUndirectedGraph(numberNodes.get(i) + 1);

            // Put the nodes
            NodesMapper<String> mapper = new NodesMapper<String>();
            gArray[i] = addNodesGraph(gArray[i], i, "src/main/resources/yes_graph.txt", mapper);

            //normalize the weights
            gArray[i] = normalizeGraph(gArray[i]);

            AtomicDouble[] info = GraphInfo.getGraphInfo(gArray[i], 1);
            System.out.println("Nodes:" + info[0]);
            System.out.println("Edges:" + info[1]);
            System.out.println("Density:" + info[2]);

            
            // extract remove the edges with w<t
            gArray[i] = SubGraphByEdgesWeight.extract(gArray[i], t, 1);
            
            // get the largest CC
            WeightedUndirectedGraph s = getLargestCC(gArray[i]);
            
            // To Do: export the graph s and use mapped to get back the words:
            // word = mapper.getNode(nodeID + 1)
 
            
            
            
            
            //System.out.println(Arrays.deepToString(s.out));

            // save
            
            // Get the inner core
            //WeightedUndirectedGraph sk = kcore(gArray[i]);
            //System.out.println(Arrays.deepToString(sk.weights));
        }

    }

}
