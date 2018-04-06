/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.

    Running this file will create the temporal distribution of tweets of the politicians
    under src/main/resources/histogram*.txt
 */
package Analysis;

import IO.GzipReader;
import PreProcess.Parser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import twitter4j.JSONException;
import twitter4j.JSONObject;


public class DistributionOverTime {

    public static void listToTxt(String filePath, List<String> arr) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        for (String str : arr) {
            writer.write(str);
            writer.write("\n");
        }
        writer.close();
    }

    public static void createHistogram(List<List> doubleList, String filePath) throws IOException {
        List<String> finalList = new ArrayList<>();
        for (List<String> list1 : doubleList) {
            for (String t : list1) {
                finalList.add(t);
            }
        }
        System.out.print(finalList.size());

        listToTxt(filePath, finalList);
    }

    public static File[] findFilesInDirectory(String directoryPath) {
        // Finds all files and folders in a path and returns them
        File folder = new File(directoryPath);

        File[] files = folder.listFiles();
        /* 
        for (File file : files) 
        {
            if (file.isDirectory())
            {
                System.out.println(file.getName());
            }
        }*/
        return files;
    }

    public static List<String> txtToArray(String filename) throws IOException {
        // read the file and return an array that each entry is a politician twitter name
        BufferedReader abc = new BufferedReader(new FileReader(filename));
        List<String> data = new ArrayList<String>();
        String s;
        while ((s = abc.readLine()) != null) {
            data.add(s);
            //System.out.println(s);
        }
        abc.close();
        //data.forEach(System.out::println);
        return data;
    }

    public static List<List<String>> getHistogram(String filenameTweets, String filenameYes, String filenameNo) throws IOException, JSONException {
        // list of candidates
        List<String> listYes = txtToArray(filenameYes);
        List<String> listNo = txtToArray(filenameNo);
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
        String filePathYes = "src/main/resources/histogramYes.txt";
        String filePathNo = "src/main/resources/histogramNo.txt";

        // it means that the user has provided a filename
        if (args.length != 0) {
            folderStream = args[0];
            filenameYes = args[1];
            filenameNo = args[2];

        } else {
            folderStream = "src/main/resources/sbn-data/stream";
            //folderStream = "/home/sergio/Escritorio/sbnData/sbn-data/stream";
            filenameYes = "src/main/resources/politicians/yes.txt";
            filenameNo = "src/main/resources/politicians/no.txt";
        }

        //Check where the tweets are
        File[] folders = findFilesInDirectory(folderStream);

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
            File[] files = findFilesInDirectory(folder.getPath());

            //go into each file
            for (File file : files) {
                filenameTweets = file.getPath();
                histograms = getHistogram(filenameTweets, filenameYes, filenameNo);
                histogramYes.add(histograms.get(0));
                histogramNo.add(histograms.get(1));
            }

        }

        //System.out.print(histogramYes);
        //System.out.println("");
        //System.out.print(histogramNo);
        System.out.println("The yes to referendum has this number of tweets");
        createHistogram(histogramYes, filePathYes);
        System.out.println("\n\nThe no to referendum has this number of tweets");
        createHistogram(histogramNo, filePathNo);
    }
}
