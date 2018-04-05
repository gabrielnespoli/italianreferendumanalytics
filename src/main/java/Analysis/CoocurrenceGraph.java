/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 *
 * @author sergio
 */
public class CoocurrenceGraph {

    public static void Write(List<String> s1, List<String> s2, List<Integer> w, List<Integer> c, String fn) {

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
            System.out.println((float) i/lines.length*100 + "%");
            String[] partsi = lines[i].split(" ");
            int resulti = Integer.parseInt(partsi[1]);
            for (int j = i ; j < lines.length; j++) {
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

        Write(col1, col2, weight, c, outputFile);
        System.out.println("Done");

    }

    public static void main(String[] args) throws IOException, ParseException, Exception {
        String clusterYes = "src/main/resources/yesClusters.txt";
        String clusterNo = "src/main/resources/noClusters.txt";

        // Load the index
        // Yes tweets
        String indexFolderYes = "src/main/resources/yes_index/";
        // No tweets
        String indexFolderNo = "src/main/resources/no_index/";

        generateGraph(clusterYes, indexFolderYes, "src/main/resources/yes_graph.txt");
        generateGraph(clusterNo, indexFolderNo, "src/main/resources/no_graph.txt");

    }

}
