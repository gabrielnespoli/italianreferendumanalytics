package Analysis;

import IO.ReadFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import static org.apache.lucene.util.Version.LUCENE_41;

public abstract class CoocurrenceGraph {

    public static final String RESOURCES_DIRECTORY = "src/main/resources/";
    public static final String OUTPUT_GRAPH_DIRECTORY = "src/main/resources/";
    public static final String INDEX_DIRECTORY = "src/main/resources/";
    public static final String CLUSTER_DIRECTORY = "src/main/resources/";

    public static void writeCoocurrenceGraph(List<String> s1, List<String> s2, List<Integer> w, List<Integer> c, String fn) {

        try {
            FileWriter fr = new FileWriter(fn);
            BufferedWriter br = new BufferedWriter(fr);
            PrintWriter out = new PrintWriter(br);
            for (int i = 0; i < s1.size(); i++) {
                if (s1.get(i) != null) {
                    out.write(s1.get(i) + " " + s2.get(i) + " " + w.get(i) + " " + c.get(i));
                }
                out.write("\n");
            }
            out.close();

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static int countCoocurrence(String term1, String term2, IndexReader ir) throws IOException, ParseException {
        // Given 2 terms and an index, calculates how many documents contains both words
        IndexSearcher searcher = new IndexSearcher(ir);
        Analyzer analyzer = new StandardAnalyzer(LUCENE_41);
        QueryParser parser = new QueryParser(LUCENE_41, "term", analyzer);
        String sq = "+" + term1 + " +" + term2;
        //System.out.println(sq);
        Query q = parser.parse(sq);
        //Query q;
        //q = new TermQuery( new Term("term","casa"));

        TopDocs top = searcher.search(q, 1000000);
        ScoreDoc[] hits = top.scoreDocs;
        return hits.length;
    }

    public static void generateGraph(String cluster, String indexFolder, String outputFile) throws IOException, ParseException, Exception {

        File directoryIndex = new File(indexFolder);
        IndexReader ir = DirectoryReader.open(FSDirectory.open(directoryIndex));

        // Save here the graph
        List<String> col1 = new ArrayList<>();
        List<String> col2 = new ArrayList<>();
        List<Integer> weight = new ArrayList<>();
        List<Integer> c = new ArrayList<>();

        ReadFile rf = new ReadFile();

        String[] lines = rf.readLines(cluster);

        for (int i = 0; i < lines.length; i++) {
            System.out.println((float) i / lines.length * 100 + "%");
            String[] partsi = lines[i].split(" ");
            int resulti = Integer.parseInt(partsi[1]);
            for (int j = i; j < lines.length; j++) {
                String[] partsj = lines[j].split(" ");
                int resultj = Integer.parseInt(partsj[1]);
                if (resultj == resulti && !partsi[0].equals(partsj[0])) {
                    int count = countCoocurrence(partsi[0], partsj[0], ir);
                    //System.out.println(count);
                    if (count > 0) {
                        col1.add(partsi[0]);
                        col2.add(partsj[0]);
                        weight.add(count);
                        c.add(resultj);
                    }

                }
            }
        }

        writeCoocurrenceGraph(col1, col2, weight, c, outputFile);
        System.out.println("Done");

    }

    public static void generateCoocurrenceGraph() throws IOException, ParseException, Exception {
        String clusterYes = CLUSTER_DIRECTORY + "yesClusters.txt";
        String clusterNo = CLUSTER_DIRECTORY + "noClusters.txt";

        // Load the index
        String indexFolderYes = INDEX_DIRECTORY + "yes_index/";  // Yes tweets
        String indexFolderNo = INDEX_DIRECTORY + "no_index/";  // No tweets

        generateGraph(clusterYes, indexFolderYes, OUTPUT_GRAPH_DIRECTORY + "yes_graph.txt");
        generateGraph(clusterNo, indexFolderNo, OUTPUT_GRAPH_DIRECTORY + "no_graph.txt");
    }

}