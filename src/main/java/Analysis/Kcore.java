/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Analysis;

import com.google.common.util.concurrent.AtomicDouble;
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
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 *
 * @author sergio
 */
public class Kcore {

    public static void main(String[] args) throws IOException, ParseException, Exception {
        int worker = (int) (Runtime.getRuntime().availableProcessors());

        // minimum value of the weight of an edge to keep the edge
        double t = 0.01;

        // load the graph from what we did in CoocurrenceGraph (counts the number of
        // times that 2 words appear in the same document
        String graph = "src/main/resources/yes_graph.txt";
        ReadFile rf = new ReadFile();
        String[] lines = rf.readLines(graph);

        // map the words into id for g stilo
        NodesMapper<String> mapper = new NodesMapper<String>();
        System.out.println("The number lines of the file is " + lines.length);

        // creathe the graph
        // keep in mind that the id of a word is mapper.getId(s1) - 1 (important the -1)
        int n = lines.length;
        WeightedDirectedGraph g = new WeightedDirectedGraph(1000 + 1);
        for (int i = 0; i < n; i++) {
            // split the line in 3 parts: node1, node2, and weight
            String[] line = lines[i].split(" ");
            String node1 = line[0];
            String node2 = line[1];
            Double w = Double.parseDouble(line[2]);
            // the graph is directed, add links in both ways
            g.add(mapper.getId(node1) - 1, mapper.getId(node2) - 1, w);
            g.add(mapper.getId(node2) - 1, mapper.getId(node1) - 1, w);
        }

        // get info about the graph using the stilo library
        System.out.println();
        System.out.println("Info about the graph:");
        AtomicDouble[] info = GraphInfo.getGraphInfo(g, worker);
        System.out.println("Nodes:" + info[0]);
        System.out.println("Edges:" + info[1]);
        System.out.println("Density:" + info[2]);

        // Normalize the weights
        double suma = 0;
        // go in each node
        for (int i = 0; i < info[0].intValue(); i++) {
            suma = 0;
            // calculate the sum of the weights of this node with the neighbours
            for (int j = 0; j < g.weights[i].length; j++) {
                suma = suma + g.weights[i][j];
            }

            // update the weights by dividing by the total weight sum
            for (int j = 0; j < g.weights[i].length; j++) {
                g.weights[i][j] = g.weights[i][j] / suma;
                //g.update(i, j,g.weights[i][j] / suma);
            }

            // make the threshole: if the weight is less than t,
            // put it as 0 because I dont see a method for removing the edge
            /*for (int j = 0; j < g.weights[i].length; j++) {
                if (g.weights[i][j] < t) {
                    //g.weights[i][j] = (double) 0;
                    g.update(i, j, (double) 0);
                }
            }*/
        }

        // Create a subgraph from the edges that have at least weight t
        g = SubGraphByEdgesWeight.extract(g, t, 8);

        System.out.println();
        System.out.println("Info about the graph:");
        AtomicDouble[] info2 = GraphInfo.getGraphInfo(g, worker);
        System.out.println("Nodes:" + info2[0]);
        System.out.println("Edges:" + info2[1]);
        System.out.println("Density:" + info2[2]);

        // Show the weights as list of lists:
        // [[weight of node1 to node1, weight of node1 to node2, weight of node1 to node3, ...], [weight of node2 to node1, [weight of node2 to node2, [weight of node2 to node3, ...]]
        System.out.println();
        System.out.println(Arrays.deepToString(g.weights));

        // get the list of all nodes for the rootedConnectedComponents method (to indentify conected components)
        // example here https://github.com/giovanni-stilo/G/blob/master/src/main/java/it/stilo/g/example/CCExample.java
        int[] all = new int[g.size];
        for (int i = 0; i < g.size; i++) {
            all[i] = i;
        }
        Set<Set<Integer>> comps = ConnectedComponents.rootedConnectedComponents(g, all, worker);
        //Set<Set<Integer>> comps = ConnectedComponents.rootedConnectedComponents(g, new int[]{1,2}, worker);
        System.out.println();
        System.out.println("--- Number of Connected Components");
        System.out.println(comps.size());
        //Run the K-core
        // example here https://github.com/giovanni-stilo/G/blob/master/src/main/java/it/stilo/g/example/CorenessExample.java
        // and method getInnerMostCore here https://github.com/giovanni-stilo/G/blob/master/src/main/java/it/stilo/g/algo/CoreDecomposition.java
        System.out.println();
        System.out.println("--- Top inner most core");

        // iterate on each CC
  
        
        for (Set<Integer> innerSet : comps) {
            
            // the method SubGraph.extract needs primitive int[], so convert from set
            int[] subnodes = new int[innerSet.size()];
            Iterator<Integer> iterator = innerSet.iterator();
            for (int j = 0; j < subnodes.length; j++) {
                subnodes[j] = iterator.next().intValue();
            }

            WeightedDirectedGraph s = SubGraph.extract(g, subnodes, worker);
            Core cc = CoreDecomposition.getInnerMostCore(s, worker);
            System.out.println();
            System.out.println(innerSet);
            System.out.println("Minimum degree: " + cc.minDegree);
            System.out.println("Vertices: " + cc.seq.length);
        }

    }

}
