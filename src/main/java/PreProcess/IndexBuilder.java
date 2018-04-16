package PreProcess;

import static Analysis.TemporalAnalysis.STOPWORDS;
import static Analysis.TemporalAnalysis.toLuceneDocument;
import IO.CSVUtils;
import IO.GzipReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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

public abstract class IndexBuilder {

    public static final String STREAM_FILES_LOCATION = "src/main/resources/sbn-data/stream/";
    public static final File[] SUB_DIRECTORIES = new File(STREAM_FILES_LOCATION).listFiles((File file) -> file.isDirectory());

    public static final String RESOURCES_DIRECTORY = "src/main/resources/";
    public static final String INDEX_DIRECTORY = "src/main/resources/";
    public static final String STOPWORDS_FILENAME = "stopwords.txt";

    public static HashMap<String, LinkedHashSet<String>> loadPoliticiansNames() {
        LinkedHashSet<String> yes_supporters = new LinkedHashSet<>();
        LinkedHashSet<String> no_supporters = new LinkedHashSet<>();

        String csvFile = "src/main/resources/politicians.csv";
        List<String[]> politicians = CSVUtils.readCSV(csvFile, ";");

        politicians.forEach((politicianPair) -> {
            yes_supporters.add(politicianPair[0]);
            if (politicianPair.length != 1) {
                no_supporters.add(politicianPair[1]);
            }
        });

        HashMap<String, LinkedHashSet<String>> supporters = new HashMap<>();
        supporters.put("yes", yes_supporters);
        supporters.put("no", no_supporters);
        return supporters;
    }

    public static IndexWriter createEmptyIndex(String directory) throws IOException {
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41, STOPWORDS);
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_41, analyzer);
        File yesIndexFile = new File(INDEX_DIRECTORY + directory);
        FSDirectory yesIndex = FSDirectory.open(yesIndexFile);
        return new IndexWriter(yesIndex, cfg);
    }

    public static void createIndexAllTweets() throws IOException, ParseException, java.text.ParseException, JSONException {
        JSONObject json;
        BufferedReader br;
        String tweet;
        Document document;

        IndexWriter IndexWriter = createEmptyIndex("all_tweets_index/");
        double counter = 0.0;
        for (File subDirectory : SUB_DIRECTORIES) {

            System.out.println(counter / SUB_DIRECTORIES.length * 100.0 + " % Done - Reading files from the directory " + subDirectory.getName());
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

    public static void createYesNoIndex() throws IOException, ParseException, java.text.ParseException, JSONException {
        JSONObject json;
        BufferedReader br;
        String tweet;
        Document document;

        IndexWriter yesIndexWriter = createEmptyIndex("yes_index/");
        IndexWriter noIndexWriter = createEmptyIndex("no_index/");

        HashMap<String, LinkedHashSet<String>> politicians; //[0] = yes_supporters, [1] = no_supporters
        politicians = loadPoliticiansNames();
        double counter = 0.0;
        for (File subDirectory : SUB_DIRECTORIES) {
            System.out.println(counter / SUB_DIRECTORIES.length * 100.0 + " % Done - Reading files from the directory " + subDirectory.getName());
            counter += 1.0;

            System.out.println("Reading files from the directory " + subDirectory.getName());

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

                    //just accept for the analysis the tweets from the politicians
                    if (politicians.get("yes").contains(user) || politicians.get("yes").contains(rtUser)) {
                        tweet = (String) json.get("text");
                        document = toLuceneDocument(tweet, user, rtUser, (String) json.get("created_at")); //create the Lucene Document
                        yesIndexWriter.addDocument(document); // add the document to the index
                    } else if (politicians.get("no").contains(user) || politicians.get("no").contains(rtUser)) {
                        tweet = (String) json.get("text");
                        document = toLuceneDocument(tweet, user, rtUser, (String) json.get("created_at")); //create the Lucene Document
                        noIndexWriter.addDocument(document); // add the document to the index
                    }
                }
            }
        }

        yesIndexWriter.commit(); // write the index to the file opened to "store" the index
        yesIndexWriter.close();
        noIndexWriter.commit(); // write the index to the file opened to "store" the index
        noIndexWriter.close();
    }

}