/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PreProcess;

import Utils.CSVUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class FindPoliticians {

    private static final int FOLLOWERSTHRESHOLD = 5000;

    private static Twitter getTwitter() {
        ConfigurationBuilder cfg = new ConfigurationBuilder();
        cfg.setOAuthAccessToken("804265252221308928-QMQRe5XZPRSBmXqZTYD1ESzOoiNDY7y");
        cfg.setOAuthAccessTokenSecret("ykK4tbpXus9Yggh41ChMnbct4mHjbc0gDxEcyOYerX9SD");
        cfg.setOAuthConsumerKey("42Q1oVVQ8wDmWRPAk8tx0lMRk");
        cfg.setOAuthConsumerSecret("MIjyDlLibLTwpZgUzIlttUvzhs3tdwDLwj7j5WPgQ3galoC7mK");
        TwitterFactory tf = new TwitterFactory(cfg.build());
        return tf.getInstance();
    }

    private static QueryResult findTweetsAboutReferendum(Twitter twitter) throws TwitterException {
        Query query = new Query("#referendum");
        query.setSince("2016-09-01");
        query.setUntil("2016-12-05");

        QueryResult result = twitter.search(query);
        if (result.getTweets() == null || result.getTweets().isEmpty()) {
            return null;
        }
        return result;
    }

    private static String fromNameToTwitterID(String name) throws InterruptedException {
        Twitter twitter = getTwitter();
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
                Logger.getLogger(FindPoliticians.class.getName()).log(Level.SEVERE, null, ex);
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
        List<String[]> politicians = CSVUtils.readCSV();
        politicians = fromMatrixNameToMatrixTwitterID(politicians);
        CSVUtils.writeCSV(politicians);
    }
}
