/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PreProcess;

import IO.CSVUtils;
import Utils.TwitterUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public abstract class PoliticiansLoader {

    public static final int FOLLOWERSTHRESHOLD = 5000;

    private static String fromNameToTwitterID(String name) throws InterruptedException {

        Twitter twitter = TwitterUtils.getTwitter();
        boolean done = true;
        do {
            try {
                User user = twitter.searchUsers(name, 0).get(0);
                if (user.getFollowersCount() >= FOLLOWERSTHRESHOLD) {
                    name = "@" + user.getScreenName();
                } else {
                    name = "";
                }
            } catch (TwitterException ex) {
                Logger.getLogger(PoliticiansLoader.class.getName()).log(Level.SEVERE, null, ex);
                TimeUnit.MINUTES.sleep(3);
                done = false;
            } catch (java.lang.IndexOutOfBoundsException e) {
                name = "";
            }
        } while (!done);
        
        return name;
    }

    private static List<String[]> fromMatrixNameToMatrixTwitterID(List<String[]> politiciansMatrix) {
        politiciansMatrix.forEach((politiciansLine) -> {
            //jump the header
            if (politiciansLine != politiciansMatrix.get(0)) {
                String politicianYes = politiciansLine[0];
                String politicianNo = "";

                if (politiciansLine.length > 1) {
                    politicianNo = politiciansLine[1];
                }

                try {
                    if (!politicianYes.startsWith("@")) {
                        politicianYes = fromNameToTwitterID(politicianYes);
                        politiciansLine[0] = politicianYes;
                    }
                    if (!politicianNo.startsWith("@") && !"".equals(politicianNo)) {
                        politicianNo = fromNameToTwitterID(politicianNo);
                        politiciansLine[1] = politicianNo;
                    }
                } catch (InterruptedException ie) {
                    System.out.println(ie.getMessage());
                }
            }
        });
        
        return politiciansMatrix;
    }

    public static void main(String[] args) throws TwitterException, FileNotFoundException, IOException {
        String csvFile = "src/main/resources/politicians.csv";
        List<String[]> politicians = CSVUtils.readCSV(csvFile, ";");
        politicians = fromMatrixNameToMatrixTwitterID(politicians);
        CSVUtils.writeCSV(politicians, "src/main/resources/politicians_loaded.csv");
    }
}
