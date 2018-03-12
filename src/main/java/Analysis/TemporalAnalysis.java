package Analysis;

import Utils.CSVUtils;
import static Utils.CSVUtils.INDEX_DIRECTORY;
import Utils.GzipReader;
import Utils.Parser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.seninp.jmotif.sax.SAXException;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;
import org.apache.commons.text.similarity.HammingDistance;

public class TemporalAnalysis {

    public static String streamFilesLocation = "src/main/resources/sbn-data/stream/";
    public static File[] subDirectories = new File(streamFilesLocation).listFiles((File file) -> file.isDirectory());
    public static String resourcesLocation = "src/main/resources/";

    private static HashMap<String, HashMap<String, Integer>> loadPoliticiansNames() {
        HashMap<String, Integer> yes_supporters = new HashMap<>();
        HashMap<String, Integer> no_supporters = new HashMap<>();

        CSVUtils.csvFile = "politicians_loaded.csv";
        List<String[]> politicians = CSVUtils.readCSV();

        politicians.forEach((politicianPair) -> {
            yes_supporters.put(politicianPair[0], 1);
            if (politicianPair.length != 1) {
                no_supporters.put(politicianPair[1], 1);
            }
        });

        HashMap<String, HashMap<String, Integer>> supporters = new HashMap<>();
        supporters.put("yes", yes_supporters);
        supporters.put("no", no_supporters);
        return supporters;
    }

    public static Document toLuceneDocument(String tweet, String user, String rtUser, String dateTimeStr) throws IOException, java.text.ParseException {
        SimpleDateFormat f = new SimpleDateFormat("dd-MMMM-yyyy HH:mm:ss", Locale.US); //define the date format
        Document document = new Document();
        document.add(new TextField("term", tweet, Field.Store.YES));
        document.add(new StringField("user", user, Field.Store.YES));
        document.add(new StringField("rt_user", rtUser, Field.Store.YES));

        String[] dateFields = dateTimeStr.split(" ");
        dateTimeStr = dateFields[2] + "-" + dateFields[1] + "-" + dateFields[5] + " " + dateFields[3];
        document.add(new LongField("created_at", (f.parse(dateTimeStr)).getTime(), Field.Store.YES)); //convert the date to Long
        return document;
    }

    private static void createIndex() throws IOException, ParseException, java.text.ParseException, JSONException {
        JSONObject json;
        BufferedReader br;
        String tweet;
        Document document;

        // create the indexes (one for the yes supporters and another for the no)
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41);
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_41, analyzer);
        File yesIndexFile = new File(INDEX_DIRECTORY + "yes_index/");
        FSDirectory yesIndex = FSDirectory.open(yesIndexFile);
        IndexWriter yesIndexWriter = new IndexWriter(yesIndex, cfg);
        File noIndexFile = new File(INDEX_DIRECTORY + "no_index/");
        FSDirectory noIndex = FSDirectory.open(noIndexFile);
        IndexWriter noIndexWriter = new IndexWriter(noIndex, cfg);

        HashMap<String, HashMap<String, Integer>> politicians; //[0] = yes_supporters, [1] = no_supporters
        politicians = loadPoliticiansNames();
        for (File subDirectory : subDirectories) {
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
                    if (politicians.get("yes").containsKey(user) || politicians.get("yes").containsKey(rtUser)) {
                        tweet = (String) json.get("text");
                        document = toLuceneDocument(tweet, user, rtUser, (String) json.get("created_at")); //create the Lucene Document
                        yesIndexWriter.addDocument(document); // add the document to the index
                    } else if (politicians.get("no").containsKey(user) || politicians.get("no").containsKey(rtUser)) {
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

    private static long[] getMinMaxDates() throws IOException {
        File indexFile = new File(INDEX_DIRECTORY + "yes_index");
        FSDirectory indexFSDirectory = FSDirectory.open(indexFile);
        IndexReader indexReader = DirectoryReader.open(indexFSDirectory);
        Document doc;

        Long minDate = Long.MAX_VALUE;
        Long maxDate = Long.MIN_VALUE;
        long date;

        //GET THE MINIMUM TIME AND MAXIMUM TIME TO DEFINE THE TIME SLOT VECTOR
        for (int i = 0; i < indexReader.maxDoc(); i++) {
            doc = indexReader.document(i);
            if ((date = Long.parseLong(doc.get("created_at"))) < minDate) {
                minDate = date;
            }
            if ((date = Long.parseLong(doc.get("created_at"))) > maxDate) {
                maxDate = date;
            }
        }

        indexFile = new File(INDEX_DIRECTORY + "no_index");
        indexFSDirectory = FSDirectory.open(indexFile);
        indexReader = DirectoryReader.open(indexFSDirectory);

        for (int i = 0; i < indexReader.maxDoc(); i++) {
            doc = indexReader.document(i);
            if ((date = Long.parseLong(doc.get("created_at"))) < minDate) {
                minDate = date;
            }
            if ((date = Long.parseLong(doc.get("created_at"))) > maxDate) {
                maxDate = date;
            }
        }

        long[] minMaxDates = new long[2];
        minMaxDates[0] = minDate;
        minMaxDates[1] = maxDate;
        return minMaxDates;
    }

    // return an index in memory with the top N terms that are present in the index where the tweets are stores (the main index)
    private static TermStats[] getNTopTermsIndex(int N, String indexDirectory) throws IOException, Exception {
        File indexFile = new File(indexDirectory);
        FSDirectory indexFSDirectory = FSDirectory.open(indexFile);
        IndexReader indexReader = DirectoryReader.open(indexFSDirectory);
        return HighFreqTerms.getHighFreqTerms(indexReader, N, "term");
    }

    /*
    workaround to process the tweet just like lucene does before adding the terms to the index
    I couldn't do the same steps manually, so I add a tweet to a "fake index" in RAM just to process the terms
    and then I recover the terms added to this index
     */
    private static LinkedHashSet<String> getTokens(String tweet) throws IOException, Exception {
        RAMDirectory ramDir = new RAMDirectory();
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41);
        IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_41, analyzer);
        IndexWriter ramIW = new IndexWriter(ramDir, conf);
        Document doc = new Document();
        doc.add(new TextField("term", tweet, Field.Store.YES));
        ramIW.addDocument(doc);
        ramIW.commit();

        IndexReader ramIR = DirectoryReader.open(ramDir);
        LinkedHashSet<String> setTerms = new LinkedHashSet<>();
        for (TermStats term : HighFreqTerms.getHighFreqTerms(ramIR, 1000000, "term")) {
            setTerms.add(term.termtext.utf8ToString());
        }
        return setTerms;
    }

    /* 
    Create a vector of frequencies for each term in the top N terms and store in a vector in which each
    position of the vector represents and interval between maxDate and minDate
     */
    private static HashMap<String, double[]> createVectorsOfFrequencies(String directoryIndex, TermStats[] topTerms, long maxDate, long minDate, long intervalDate) throws IOException, Exception {
        //initialize the vector of frequencies inside the hashmap for all terms. The index of this vector is define as [minDate, minDate+interval, minDate+2*interval,...]
        HashMap<String, double[]> vectorOfTermsFrequencies = new HashMap<>();
        int numberSlots = (int) Math.ceil((maxDate - minDate) / intervalDate) + 1;
        for (TermStats term : topTerms) {
            vectorOfTermsFrequencies.put(term.termtext.utf8ToString(), new double[numberSlots]);
        }

        /*
        iterate through all documents, getting all terms. If the term is one of the topN, then increment the 
        frequency of the term in that interval. Each interval (usually 12h) is one position of the vector 'vecFrequencies'.
         */
        File indexFile = new File(directoryIndex);
        FSDirectory indexFSDirectory = FSDirectory.open(indexFile);
        IndexReader indexReader = DirectoryReader.open(indexFSDirectory);
        Document doc;

        LinkedHashSet terms;
        int vectorIndex;
        Long tweetDate;
        double[] vecFrequencies; //the index of this vector is define as [minDate, minDate+interval, minDate+2*interval,...]
        for (int i = 0; i < indexReader.maxDoc(); i++) {
            doc = indexReader.document(i);
            tweetDate = new Long(doc.get("created_at"));
            vectorIndex = (int) Math.floor((tweetDate - minDate) / intervalDate); //define the position of the timeseries vector
            // iterate over all the words of the tweet. The words are text-processed
            for (String term : getTokens(doc.get("term"))) {
                if (vectorOfTermsFrequencies.containsKey(term)) {
                    // update the frequency in the timeseries of the term
                    vecFrequencies = vectorOfTermsFrequencies.get(term);
                    vecFrequencies[vectorIndex]++;
                    vectorOfTermsFrequencies.put(term, vecFrequencies);
                }
            }
        }
        return vectorOfTermsFrequencies;
    }

    // create a SAX string for each term in the hashmap
    private static HashMap<String, String> fromFreqVectorsToSAXStrings(HashMap<String, double[]> vectorOfFrequencies) throws SAXException {
        HashMap<String, String> hmTermSAX = new HashMap<>();

        vectorOfFrequencies.keySet().forEach((term) -> {
            String sax;
            try {
                SAXBuilder saxUtils = new SAXBuilder(20, 0.01, new NormalAlphabet());
                sax = saxUtils.buildSAX(vectorOfFrequencies.get(term));
                hmTermSAX.put(term, sax);
            } catch (SAXException ex) {
                System.out.println(ex.getMessage());
            }
        });
        return hmTermSAX;
    }

    private static void saveClusterOnHardDisk(ArrayList<LinkedHashSet<String>> clusters, String destFolder) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(destFolder));

        for (int i = 0; i < clusters.size(); i++) {
            for (String term : clusters.get(i)) {
                pw.println(term + " " + i);
            }
        }
        pw.close();
    }

    /*
    useCache define if the program has to process the entire twitter DB. N is the number of the top terms to
    be considered in the analysis. timeInterval is the granularity of the temporal analysis used to create
    the vector of frequencies of the top N terms.
    This method will create for each top N term a SAX string that represents the frequency of this term in each
    timeInterval and will clusterize the SAX strings that have the same temporal behavior (almost the same SAX string)
    
    The result of the clustering will be saved in /src/main/resources/cluster in the format:
    <term1> <cluster_id>
    <term2> <cluster_id>...
     */
    public static void clusterTopNTerms(boolean useCache, int N, int timeInterval) throws IOException, Exception {
        if (!useCache) {
            createIndex();
        }

        TermStats[] yesNTopTerms = getNTopTermsIndex(N, INDEX_DIRECTORY + "yes_index");
        TermStats[] noNTopTerms = getNTopTermsIndex(N, INDEX_DIRECTORY + "no_index");

        long[] minMaxDates = getMinMaxDates();
        long min = minMaxDates[0];
        long max = minMaxDates[1];
        //calculate the interval in type long
        Calendar c = Calendar.getInstance(Locale.US);
        c.setTime(new Date(min));
        c.add(Calendar.HOUR, timeInterval);
        long interval = c.getTime().getTime() - min;

        HashMap<String, double[]> hmYesTermFreqVector = createVectorsOfFrequencies(INDEX_DIRECTORY + "yes_index", yesNTopTerms, max, min, interval);
        HashMap<String, double[]> hmNoTermFreqVector = createVectorsOfFrequencies(INDEX_DIRECTORY + "no_index", noNTopTerms, max, min, interval);

        //for each term create a SAX that represents the frequencies vector
        HashMap<String, String> hmYesTermSAX = fromFreqVectorsToSAXStrings(hmYesTermFreqVector);
        HashMap<String, String> hmNoTermSAX = fromFreqVectorsToSAXStrings(hmNoTermFreqVector);

        HammingDistance hd = new HammingDistance();
        KMeans kmeans = new KMeans(hd);
        ArrayList<LinkedHashSet<String>> yesClusters = kmeans.getClusterOfTerms(7, hmYesTermSAX);
        ArrayList<LinkedHashSet<String>> noClusters = kmeans.getClusterOfTerms(7, hmNoTermSAX);

        saveClusterOnHardDisk(yesClusters, resourcesLocation + "yesClusters.txt");
        saveClusterOnHardDisk(noClusters, resourcesLocation + "noClusters.txt");
    }

}
