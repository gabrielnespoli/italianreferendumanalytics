/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Analysis;

import static Analysis.TemporalAnalysis.INDEX_DIRECTORY;
import static Analysis.TemporalAnalysis.STOPWORDS;
import static Analysis.TemporalAnalysis.SUB_DIRECTORIES;
import static Analysis.TemporalAnalysis.SUB_DIRECTORIES_TEST;
import static Analysis.TemporalAnalysis.toLuceneDocument;
import IO.GzipReader;
import PreProcess.Parser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import static org.apache.lucene.util.Version.LUCENE_41;
import twitter4j.JSONException;
import twitter4j.JSONObject;

public class TweetsOpinion {

    public static void listToTxt(String filePath, List<String> arr) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        for (String str : arr) {
            writer.write(str);
            writer.write("\n");
        }
        writer.close();
    }

    private static void createIndexAllTweets() throws IOException, ParseException, java.text.ParseException, JSONException {
        JSONObject json;
        BufferedReader br;
        String tweet;
        Document document;

        // create the indexes (one for the yes supporters and another for the no)
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41, STOPWORDS);
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_41, analyzer);
        File IndexFile = new File(INDEX_DIRECTORY + "all_tweets_index/");
        FSDirectory Index = FSDirectory.open(IndexFile);
        IndexWriter IndexWriter = new IndexWriter(Index, cfg);
        double counter = 0.0;
        for (File subDirectory : SUB_DIRECTORIES_TEST) {

            System.out.println(counter / SUB_DIRECTORIES_TEST.length * 100.0 + " % Done - Reading files from the directory " + subDirectory.getName());
            counter += 1.0;

            // get all the files inside 'subDirectory'
            File[] files = new File(subDirectory.getAbsolutePath()).listFiles((File file) -> file.isFile());
            for (File file : files) {
                System.out.println("Reading " + file.getName());
                br = GzipReader.getBufferedReaderGzFile(file.getPath());

                //read the file, line by line 
                while ((tweet = br.readLine()) != null) {
                    json = Parser.getJSON(tweet);

                    String user = (String) ((JSONObject) json.get("user")).get("screen_name");
                    String rtUser = "";
                    if (!json.isNull("in_reply_to_screen_name")) {
                        rtUser = (String) json.get("in_reply_to_screen_name");
                    }

                    tweet = (String) json.get("text");
                    document = toLuceneDocument(tweet, user, rtUser, (String) json.get("created_at")); //create the Lucene Document
                    IndexWriter.addDocument(document); // add the document to the index

                }
            }
        }

        IndexWriter.commit(); // write the index to the file opened to "store" the index
        IndexWriter.close();
    }

    public static List<String> txtToArray(String filename) throws IOException {
        // read the file and return an array that each entry is a politician twitter name
        BufferedReader abc = new BufferedReader(new FileReader(filename));
        List<String> data = new ArrayList<String>();
        String s;
        while ((s = abc.readLine()) != null) {
            data.add(s);
        }
        abc.close();
        return data;
    }

    private static void findSupporters(String filenamePoliticians, String filenameOutput) throws IOException, ParseException {
        // Load the all tweets index and analyzer
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41, STOPWORDS);
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_41, analyzer);

        File directoryIndex = new File(INDEX_DIRECTORY + "all_tweets_index/");
        IndexReader ir = DirectoryReader.open(FSDirectory.open(directoryIndex));
        IndexSearcher searcher = new IndexSearcher(ir);
        QueryParser parser = new QueryParser(LUCENE_41, "term", analyzer);

        // Get the list of politicians
        List<String> listPoliticians = txtToArray(filenamePoliticians);
        
        // iterate over politicians to find them in the tweets
        String politician = listPoliticians.get(1);
        Query q = parser.parse("+ " + politician);

        // Find tweets. I put a very high threshold so i find all of them
        TopDocs top = searcher.search(q, 10000000);
        ScoreDoc[] hits = top.scoreDocs;

        // Go on each hit
        Document doc = null;
        
        // Save the results using this list
        List<String> saveHits = new ArrayList<String>();
        String s = "";
        
        //Unique users to show statistics
        List<String> users = new ArrayList<String>();

        // go on each hit
        for (ScoreDoc entry : hits) {
            doc = searcher.doc(entry.doc);
            //System.out.println("field1: " + doc.get("user"));
            s = doc.get("user") + " " + politician;
            saveHits.add(s);
            users.add(doc.get("user"));
        }
        
        // Save to file as <user> <polititian>
        listToTxt(filenameOutput, saveHits);
        // Show the asked data
        Set uniqueUsers = new HashSet(users);
        System.out.println("Unique users: " + uniqueUsers.size());
        System.out.println("Total tweets: " + saveHits.size());
    }

    public static void main(String[] args) throws IOException, JSONException, ParseException, java.text.ParseException {
        // Uncomment to create the inverted index of all tweets
        //createIndexAllTweets();
        System.out.println("#### YES");
        findSupporters("src/main/resources/yes_politicians.txt", "src/main/resources/yes_users_mention_politicians.txt");
        
        System.out.println("#### NO");
        findSupporters("src/main/resources/no_politicians.txt", "src/main/resources/no_users_mention_politicians.txt");
    }
}
