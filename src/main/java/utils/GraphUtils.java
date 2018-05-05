package utils;

import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.structures.WeightedUndirectedGraph;

public abstract class GraphUtils {

    public static WeightedUndirectedGraph resizeGraph(WeightedUndirectedGraph old, LongIntDict direct, int newSize) {
        WeightedUndirectedGraph g = new WeightedUndirectedGraph(newSize + 1);

        int id0 = 0, id1 = 0;
        for (int i = 0; i < old.in.length; i++) {
            if (old.in[i] != null && old.in[i].length != 0) {
                id0 = direct.testAndSet(Double.valueOf(i).longValue());
                for (int j = 0; j < old.in[i].length; j++) {
                    id1 = direct.testAndSet(Double.valueOf(old.in[i][j]).longValue());

                    // the graph is undirected, if the node has already been added before, don't add again
                    if (id0 < id1) {
                        g.add(id0, id1, 1.0);
                    }
                }
            }
        }
        return g;
    }

    public static WeightedDirectedGraph resizeGraph(WeightedDirectedGraph old, LongIntDict direct, int newSize) {
        WeightedDirectedGraph g = new WeightedDirectedGraph(newSize + 1);

        int id0 = 0, id1 = 0;
        for (int i = 0; i < old.in.length; i++) {
            if (old.in[i] != null && old.in[i].length != 0) {
                id0 = direct.testAndSet(Double.valueOf(i).longValue());
                for (int j = 0; j < old.in[i].length; j++) {
                    id1 = direct.testAndSet(Double.valueOf(old.in[i][j]).longValue());
                    g.add(id0, id1, 1.0);
                }
            }
        }

        return g;
    }
}
