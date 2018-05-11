package analysis;

import index.IndexBuilder;
import static index.IndexBuilder.INDEX_DIRECTORY;
import utils.Plotter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.TreeSet;
import net.seninp.jmotif.sax.SAXException;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import net.seninp.jmotif.sax.alphabet.NormalAlphabet;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public abstract class TemporalAnalysis {

    public static final String STREAM_FILES_LOCATION = "src/main/resources/sbn-data/stream/";
    public static final File[] SUB_DIRECTORIES = new File(STREAM_FILES_LOCATION).listFiles((File file) -> file.isDirectory());

    // Files for testing
    public static final String STREAM_FILES_LOCATION_TEST = "src/main/resources/sbn-data/stream/";
    public static final File[] SUB_DIRECTORIES_TEST = new File(STREAM_FILES_LOCATION_TEST).listFiles((File file) -> file.isDirectory());

    public static final String RESOURCES_DIRECTORY = "src/main/resources/";
    
    private static long[] getMinMaxDates() throws IOException {
        File indexFile = new File(IndexBuilder.INDEX_DIRECTORY + "yes_index");
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

    /*
    Lucene returns the top N terms in an uncommon array of objects of the type TermStats.
    This method convert the array of TermStats to a list of strings.
     */
    private static ArrayList<String> fromTermStatsToList(TermStats[] termsStats) {
        ArrayList<String> termsList = new ArrayList<>();

        for (TermStats term : termsStats) {
            termsList.add(term.termtext.utf8ToString());
        }

        return termsList;
    }

    // return an index in memory with the top N terms that are present in the index where the tweets are stores (the main index)
    private static ArrayList<String> getNTopTermsIndex(int N, String indexDirectory) throws IOException, Exception {
        File indexFile = new File(indexDirectory);
        FSDirectory indexFSDirectory = FSDirectory.open(indexFile);
        IndexReader indexReader = DirectoryReader.open(indexFSDirectory);
        TermStats[] termStats = HighFreqTerms.getHighFreqTerms(indexReader, N, "term");

        return fromTermStatsToList(termStats);
    }

    /*
    workaround to process the tweet just like lucene does before adding the terms to the index
    I couldn't do the same steps manually, so I add a tweet to a "fake index" in RAM just to process the terms
    and then I recover the terms added to this index
     */
    private static LinkedHashSet<String> getTokens(String tweet) throws IOException, Exception {
        RAMDirectory ramDir = new RAMDirectory();
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41, IndexBuilder.STOPWORDS);
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
    private static HashMap<String, double[]> createTermsTimeSeries(String indexDirectory, ArrayList<String> topTerms, long maxDate, long minDate, long intervalDate) throws IOException, Exception {
        //initialize the vector of frequencies inside the hashmap for all terms. The index of this vector is define as [minDate, minDate+interval, minDate+2*interval,...]
        HashMap<String, double[]> vectorOfTermsFrequencies = new HashMap<>();
        int numberSlots = (int) Math.ceil((maxDate - minDate) / intervalDate) + 1;
        topTerms.forEach((term) -> {
            vectorOfTermsFrequencies.put(term, new double[numberSlots]);
        });

        /*
        iterate through all documents, getting all terms. If the term is one of the topN, then increment the 
        frequency of the term in that interval. Each interval (usually 12h) is one position of the vector 'vecFrequencies'.
         */
        File indexFile = new File(indexDirectory);
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
    private static HashMap<String, String> fromFreqVectorsToSAXStrings(HashMap<String, double[]> hmTermFrequencies, int alphabetSize) throws SAXException {
        HashMap<String, String> hmTermSAX = new HashMap<>();

        // normalize the vector of frequencies, in this way there will be a better comparison
        // between 2 different SAX strings
        TreeSet allFrequencies = new TreeSet();
        hmTermFrequencies.values().forEach((double[] frequencies) -> {
            for (int i = 0; i < frequencies.length; i++) {
                allFrequencies.add(frequencies[i]);
            }
        });

        double maxFreq = (double) allFrequencies.last();

        // divide all elements by the maxFreq, normalizing, then, between 0 and 1
        hmTermFrequencies.values().forEach((double[] frequencies) -> {
            for (int i = 0; i < frequencies.length; i++) {
                frequencies[i] = frequencies[i] / maxFreq;
            }
        });

        hmTermFrequencies.keySet().forEach((term) -> {
            String sax;
            try {
                SAXBuilder saxUtils = new SAXBuilder(alphabetSize, Math.round(alphabetSize / maxFreq), new NormalAlphabet());
                sax = saxUtils.buildSAX(hmTermFrequencies.get(term));
                hmTermSAX.put(term, sax);
            } catch (SAXException ex) {
                System.out.println(ex.getMessage());
            }
        });
        return hmTermSAX;
    }

    private static void saveClusterOnHardDisk(HashMap<String, Integer> clusters, String destFolder) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(destFolder));

        clusters.keySet().forEach((saxString) -> {
            pw.println(saxString + " " + clusters.get(saxString));
        });

        pw.close();
    }

    /*
    map a cluster of SAX strings to the cluster of terms that they represent.
    For example, the cluster
        aabcad 0
        caabda 1
        ahrfaa 1
    may be converted to
        italia 0
        oggi 1
        macchina 1
     */
    private static HashMap<String, Integer> mapSAXClusterToTermCluster(HashMap<String, Integer> hmSAXClusters, HashMap<String, String> hmTermSAX) {
        HashMap<String, Integer> termClusters = new HashMap<>();
        String sax;

        // iterate through all the terms, assigning to them the correspondent cluster
        // of their SAX string
        for (String term : hmTermSAX.keySet()) {
            sax = hmTermSAX.get(term);
            termClusters.put(term, hmSAXClusters.get(sax));
        }

        return termClusters;
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
    public static void clusterTopNTerms(int N, int timeInterval, int k) throws IOException, Exception {

        ArrayList<String> yesNTopTerms = getNTopTermsIndex(N, INDEX_DIRECTORY + "yes_index");
        ArrayList<String> noNTopTerms = getNTopTermsIndex(N, INDEX_DIRECTORY + "no_index");

        long[] minMaxDates = getMinMaxDates();
        long min = minMaxDates[0];
        long max = minMaxDates[1];
        //calculate the interval in type long
        Calendar c = Calendar.getInstance(Locale.US);
        c.setTime(new Date(min));
        c.add(Calendar.HOUR, timeInterval);
        long interval = c.getTime().getTime() - min;

        HashMap<String, double[]> hmYesTermsTS = createTermsTimeSeries(INDEX_DIRECTORY + "yes_index", yesNTopTerms, max, min, interval);
        HashMap<String, double[]> hmNoTermsTS = createTermsTimeSeries(INDEX_DIRECTORY + "no_index", noNTopTerms, max, min, interval);

        //for each term create a SAX that represents the frequencies vector
        int alphabetSize = 20;
        HashMap<String, String> hmYesTermSAX = fromFreqVectorsToSAXStrings(hmYesTermsTS, alphabetSize);
        HashMap<String, String> hmNoTermSAX = fromFreqVectorsToSAXStrings(hmNoTermsTS, alphabetSize);

        KMeans yesKMeans = new KMeans(k, alphabetSize, new ArrayList<>(hmYesTermSAX.values()));
        HashMap<String, Integer> yesSAXClusters = yesKMeans.getClusters();

        KMeans noKMeans = new KMeans(k, alphabetSize, new ArrayList<>(hmNoTermSAX.values()));
        HashMap<String, Integer> noSAXClusters = noKMeans.getClusters();

        // from the SAX strings, get back to the terms, generating the clusters
        // for the terms, not the SAX that represent the term frequency vector
        HashMap<String, Integer> yesClusters = mapSAXClusterToTermCluster(yesSAXClusters, hmYesTermSAX);
        HashMap<String, Integer> noClusters = mapSAXClusterToTermCluster(noSAXClusters, hmNoTermSAX);

        saveClusterOnHardDisk(yesClusters, RESOURCES_DIRECTORY + "yesClusters.txt");
        saveClusterOnHardDisk(noClusters, RESOURCES_DIRECTORY + "noClusters.txt");
    }

    private static ImmutableTriple<ArrayList<String>, ArrayList<double[]>, ArrayList<double[]>> processTSDataToPlot(HashMap<String, double[]> hmTermsTS, int timeInterval) {
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<double[]> xvaluesList = new ArrayList<>();
        ArrayList<double[]> yvaluesList = new ArrayList<>();
        int timeSeriesSize = 0;

        // read one yvalues just to calculate the size of the time series to instantiate xvalues
        for (double[] yvalues : hmTermsTS.values()) {
            timeSeriesSize = yvalues.length;
            break;
        }

        final double[] xvalues = new double[timeSeriesSize];

        // the xvalues are the time domain, starting from 0, hopping no interval of timeInterval. The xvalues will be the same for all time series
        for (int i = 0; i < xvalues.length - 1; i++) {
            xvalues[i] = timeInterval * (i - 1);
        }

        // the yvalues are the term frequencies
        hmTermsTS.keySet().forEach((term) -> {
            labels.add(term);
            xvaluesList.add(xvalues);
            yvaluesList.add(hmTermsTS.get(term));
        });

        return new ImmutableTriple<>(labels, xvaluesList, yvaluesList);
    }

    /*
    The method generate a time series for all the terms in each cluster and plot
    a graph comparing the evolution of the terms frequency.
     */
    public static void compareTimeSeriesOfTerms(int timeInterval, String[] prefixYesNo, String[] clusterTypes) throws Exception {
        HashMap<Integer, LinkedHashSet<String>> clusters;
        ArrayList<String> terms;
        String clusterDirectory, indexDirectory, graphTitle;

        long[] minMaxDates = getMinMaxDates();
        long min = minMaxDates[0];
        long max = minMaxDates[1];
        //calculate the interval in type long
        Calendar c = Calendar.getInstance(Locale.US);
        c.setTime(new Date(min));
        c.add(Calendar.HOUR, timeInterval);
        long interval = c.getTime().getTime() - min;

        for (String prefix : prefixYesNo) {
            indexDirectory = INDEX_DIRECTORY + prefix + "_index";
            for (String clusterType : clusterTypes) {
                clusterDirectory = RESOURCES_DIRECTORY + prefix + "_" + clusterType + ".txt";
                clusters = GraphAnalysis.loadClusters(clusterDirectory);

                // iterate through all the clusters plotting the time series of all terms in the cluster
                for (Integer clusterID : clusters.keySet()) {

                    terms = new ArrayList<>(clusters.get(clusterID));
                    HashMap<String, double[]> hmTermsTS = createTermsTimeSeries(indexDirectory, terms, max, min, interval);

                    ImmutableTriple<ArrayList<String>, ArrayList<double[]>, ArrayList<double[]>> dataToPlot = processTSDataToPlot(hmTermsTS, timeInterval);

                    ArrayList<String> labels = dataToPlot.getLeft();
                    ArrayList<double[]> xvaluesList = dataToPlot.getMiddle();
                    ArrayList<double[]> yvaluesList = dataToPlot.getRight();
                    graphTitle = "Evolution of terms frequency on time (parameters: " + prefix + ", " + clusterType + ")";
                    Plotter tsPlotter = new Plotter(graphTitle, labels, xvaluesList, yvaluesList);
                    tsPlotter.plot();
                }

            }
        }

    }
}
