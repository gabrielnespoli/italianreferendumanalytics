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
import it.stilo.g.structures.Core;
import it.stilo.g.structures.WeightedUndirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 *
 * @author sergio
 */
public class Kcore {

    public static void main(String[] args) throws IOException, ParseException, Exception {
        int worker = (int) (Runtime.getRuntime().availableProcessors());
        double t = 0.01;

        String graph = "src/main/resources/yes_graph.txt";
        ReadFile rf = new ReadFile();
        String[] lines = rf.readLines(graph);
        NodesMapper<String> mapper = new NodesMapper<String>();
        System.out.println(lines.length);
        int n = lines.length;

        WeightedUndirectedGraph g = new WeightedUndirectedGraph(n + 1);

        for (int i = 0; i < n; i++) {
            String[] line = lines[i].split(" ");
            String s1 = line[0];
            String s2 = line[1];
            Double w = Double.parseDouble(line[2]);
            g.add(mapper.getId(s1) - 1, mapper.getId(s2) - 1, w);
        }
        System.out.println("Info about the graph");
        AtomicDouble[] info = GraphInfo.getGraphInfo(g, worker);
        System.out.println("Nodes:" + info[0]);
        System.out.println("Edges:" + info[1]);
        System.out.println("Density:" + info[2]);
        

        // Normalize the weights
        
        
        
        double suma = 0;
        for (int i = 0; i < info[0].intValue(); i++) {
            suma = 0;
            for (int j = 0; j < g.weights[i].length; j++) {
                suma = suma + g.weights[i][j];
            }
            
            for (int j = 0; j < g.weights[i].length; j++) {
                g.weights[i][j] = g.weights[i][j] / suma;
            }
            // make the threshole
            
            for (int j = 0; j < g.weights[i].length; j++) {
                if (g.weights[i][j] < t) {
                    g.weights[i][j] = (double) 0;
                }

            }

        }
        
        System.out.println(Arrays.deepToString(g.weights));
        int[] all = new int[g.size];
        for (int i = 0; i < g.size; i++) {
            all[i] = i;
        }
        Set<Set<Integer>> comps = ConnectedComponents.rootedConnectedComponents(g, all, worker);
        System.out.println("Components");
        System.out.println(comps.size());

        //List<Core> c = CoreDecomposition.topsInnerMost(g, worker);
        System.out.println("Decomposition");

        Core cc = CoreDecomposition.getInnerMostCore(g, worker);
        System.out.println(cc.minDegree);
        System.out.println(cc.seq.length);
    }
}
