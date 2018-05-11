package analysis;

import io.GzipReader;
import io.ReadFile;
import io.TxtUtils;
import preprocess.Parser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartUtilities;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import utils.Plotter;

public abstract class DistributionOverTime {

    public static String RESOURCES_LOCATION = "src/main/resources/";

    public static void plotHistogram(List<String> allHistLabels, List<double[]> tweetsTSDouble, int bins) {

        Plotter plotter = new Plotter("Distribution of tweets over time", "Timestamp", "Frequency", allHistLabels, tweetsTSDouble, bins);
        plotter.plot();
    }

    public static double[] loadHistogram(String filename) {

        ArrayList<Long> tweetsTimestamps = null;

        // read all the timestamps
        try {
            tweetsTimestamps = TxtUtils.txtToList(RESOURCES_LOCATION + filename, Long.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // convert the timestamps (long) to double
        double[] tweetsTSDouble = new double[tweetsTimestamps.size()];
        for (int i = 0; i < tweetsTimestamps.size(); i++) {
            tweetsTSDouble[i] = tweetsTimestamps.get(i).doubleValue();
        }

        return tweetsTSDouble;
    }

    public static void createHistogram(List<List> doubleList, String filePath) throws IOException {
        List<String> finalList = new ArrayList<>();
        for (List<String> list1 : doubleList) {
            for (String t : list1) {
                finalList.add(t);
            }
        }
        System.out.print(finalList.size());

        TxtUtils.iterableToTxt(filePath, finalList);
    }

    public static List<List<String>> getHistogram(String filenameTweets, String filenameYes, String filenameNo) throws IOException, JSONException {
        // list of candidates
        List<String> listYes = TxtUtils.txtToList(filenameYes);
        List<String> listNo = TxtUtils.txtToList(filenameNo);
        List<List<String>> nestedList = new ArrayList<>();

        BufferedReader br = GzipReader.getBufferedReaderGzFile(filenameTweets);
        String xml;
        // Make a counter that is updated everytime that a politician is found tweeting
        List<String> counterYes = new ArrayList<String>();
        List<String> counterNo = new ArrayList<String>();
        while ((xml = br.readLine()) != null) {

            //String xml = br.readLine();
            JSONObject json = Parser.getJSON(xml);
            //System.out.println(xml);

            // Get the username
            JSONObject user = json.getJSONObject("user");
            String screenName = user.getString("screen_name");

            // Get the timestamp in ms
            String timestampms = json.getString("timestamp_ms");

            // Check if the user is on one list and add the timestamp to it
            if (listYes.contains(screenName)) {
                counterYes.add(timestampms);
            }
            if (listNo.contains(screenName)) {
                counterNo.add(timestampms);
            }
            //counterYes.forEach(System.out::println);
            //System.out.println(screenName);
            nestedList.add(counterYes);
            nestedList.add(counterNo);
        }
        return nestedList;
    }

    public static void main(String[] args) throws IOException, JSONException {
        String folderStream = "";
        String filenameYes = "";
        String filenameNo = "";
        String filenameTweets = null;
        List<List> histogramYes = new ArrayList<>();
        List<List> histogramNo = new ArrayList<>();
        List<List<String>> histograms = new ArrayList<>();
        String filePathYes = RESOURCES_LOCATION + "histogramYes.txt";
        String filePathNo = RESOURCES_LOCATION + "histogramNo.txt";

        // it means that the user has provided a filename
        if (args.length != 0) {
            folderStream = args[0];
            filenameYes = args[1];
            filenameNo = args[2];

        } else {
            folderStream = RESOURCES_LOCATION + "sbn-data/stream";
            filenameYes = RESOURCES_LOCATION + "yes_politicians.txt";
            filenameNo = RESOURCES_LOCATION + "no_politicians.txt";
        }

        //Check where the tweets are
        File[] folders = ReadFile.findFilesInDirectory(folderStream);

        // Enter in each folder
        int counter = 0;
        for (File folder : folders) {
            counter++;
            System.out.println("");
            System.out.print(counter);
            System.out.print("/");
            System.out.print(folders.length);
            System.out.print(" - ");
            System.out.print(folder);
            System.out.println("");
            //find the tweet files here
            File[] files = ReadFile.findFilesInDirectory(folder.getPath());

            //go into each file
            for (File file : files) {
                filenameTweets = file.getPath();
                histograms = getHistogram(filenameTweets, filenameYes, filenameNo);
                histogramYes.add(histograms.get(0));
                histogramNo.add(histograms.get(1));
            }

        }

        System.out.println("The yes to referendum has this number of tweets");
        createHistogram(histogramYes, filePathYes);
        System.out.println("\n\nThe no to referendum has this number of tweets");
        createHistogram(histogramNo, filePathNo);
    }
}
