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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.misc.HighFreqTerms;
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

    public static int countCoocurrence(String term1, String term2, IndexReader ir) throws IOException, ParseException {
        // Given 2 terms and an index, calculates how many documents contains both words
        IndexSearcher searcher = new IndexSearcher(ir);
        Analyzer analyzer = new StandardAnalyzer(LUCENE_41);
        QueryParser parser = new QueryParser(LUCENE_41, "term", analyzer);
        String sq = "+" + term1 + " +" + term2;
        System.out.println(sq);
        Query q = parser.parse(sq);
        //Query q;
        //q = new TermQuery( new Term("term","casa"));

        TopDocs top = searcher.search(q, 1000000);
        ScoreDoc[] hits = top.scoreDocs;
        return hits.length;
    }

    public static void main(String[] args) throws IOException, ParseException, Exception {
        String clusterYes = "src/main/resources/clusterYes.csv";
        String clusterNo = "src/main/resources/clusterNo.csv";

        // Save here the graph
        List<String> col1 = new ArrayList<>();
        List<String> col2 = new ArrayList<>();
        List<Integer> weight = new ArrayList<>();

        // Load the index
        // Yes tweets
        String indexFolderYes = "src/main/resources/yes_index/";
        File directoryIndexYes = new File(indexFolderYes);
        IndexReader irYes = DirectoryReader.open(FSDirectory.open(directoryIndexYes));

        // No tweets
        String indexFolderNo = "src/main/resources/no_index/";
        File directoryIndexNo = new File(indexFolderNo);
        IndexReader irNo = DirectoryReader.open(FSDirectory.open(directoryIndexNo));

        // Test with some terms and 2 clusters
        String term1 = "matteorenz";
        String term7 = "solo";
        String term2 = "italian";
        String term6 = "https";
        String term3 = "Renzi";
        String term4 = "casa";
        List<String> cluster1 = Arrays.asList(term1, term2, term3);
        List<String> cluster2 = Arrays.asList(term4, term6, term7);

        // test 
        //System.out.println(HighFreqTerms.getHighFreqTerms(irYes, 1000, "term")[11]);
        List<List<String>> listOfClusters = new ArrayList<>();
        listOfClusters.add(cluster1);
        listOfClusters.add(cluster2);
        
        // Get into cluster1
        for (int i = 0; i < listOfClusters.size(); i++) {
        // Get into cluster 2
            for (int j = 0; j < listOfClusters.size(); j++) {
                if (j > i) {
                    // Iterate among the touples of words of both clusters
                    for (String element1 : listOfClusters.get(i)) {
                        for (String element2 : listOfClusters.get(j)) {
                            int count = countCoocurrence(element1, element2, irYes);
                            System.out.println(count);
                            col1.add(element1);
                            col2.add(element2);
                            weight.add(count);
                        }
                    }
                }

            }
        }

    }

}
