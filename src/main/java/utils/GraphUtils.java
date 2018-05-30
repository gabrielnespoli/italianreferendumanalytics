package utils;

import gnu.trove.map.TIntLongMap;
import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.structures.WeightedGraph;
import it.stilo.g.structures.WeightedUndirectedGraph;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

public abstract class GraphUtils {

    public static String SEP = "\t";

    // we were having problems with memory insufficiency. We had to resize the graph through a mapping
    // and now we have to save using the TwitterID (another mapping)
    public static void saveDirectGraph2Mappings(WeightedGraph g, String outputGraph, TIntLongMap int2LongResizeMap, TIntLongMap int2LongTwitterMap) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputGraph))));

        String newLine = "";
        int[] vertices = g.getVertex();
        for (int i = 0; i < vertices.length; i++) {
            int v = vertices[i];
            if (g.out[v] != null) {
                for (int j = 0; j < g.out[v].length; j++) {
                    int d = g.out[v][j];
                    double w = g.weights[v][j];
                    newLine = "" + int2LongTwitterMap.get((int) int2LongResizeMap.get(v)) + SEP + int2LongTwitterMap.get((int) int2LongResizeMap.get(d)) + SEP + w;

                    bw.write(newLine);
                    bw.newLine();
                }
            }
        }

        bw.flush();
        bw.close();
    }

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
