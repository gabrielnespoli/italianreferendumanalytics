package structure;

import gnu.trove.map.TIntLongMap;
import it.stilo.g.structures.WeightedGraph;

public class MappedWeightedGraph {

    private WeightedGraph g;
    private TIntLongMap map;

    public MappedWeightedGraph(WeightedGraph g, TIntLongMap map) {
        this.g = g;
        this.map = map;
    }

    public WeightedGraph getWeightedGraph() {
        return this.g;
    }

    public TIntLongMap getMap() {
        return this.map;
    }

    public void setWeightedGraph(WeightedGraph g) {
        this.g = g;
    }

    public void setMap(TIntLongMap map) {
        this.map = map;
    }
}
