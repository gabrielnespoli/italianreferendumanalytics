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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import twitter4j.JSONException;
import twitter4j.JSONObject;

public class TweetsOpinion {

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
            
            System.out.println(counter/SUB_DIRECTORIES_TEST.length*100.0 + " % Done - Reading files from the directory " + subDirectory.getName());
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

    public static void main(String[] args) throws IOException, JSONException, ParseException, java.text.ParseException {
        createIndexAllTweets();
    }
}
