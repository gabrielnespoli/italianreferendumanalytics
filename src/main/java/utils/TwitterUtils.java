package utils;

import preprocess.PoliticiansLoader;
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

public abstract class TwitterUtils {

    public static final int FOLLOWERSTHRESHOLD = 5000;

    public static twitter4j.Twitter getTwitter() {
        ConfigurationBuilder cfg = new ConfigurationBuilder();
        cfg.setOAuthAccessToken("804265252221308928-QMQRe5XZPRSBmXqZTYD1ESzOoiNDY7y");
        cfg.setOAuthAccessTokenSecret("ykK4tbpXus9Yggh41ChMnbct4mHjbc0gDxEcyOYerX9SD");
        cfg.setOAuthConsumerKey("42Q1oVVQ8wDmWRPAk8tx0lMRk");
        cfg.setOAuthConsumerSecret("MIjyDlLibLTwpZgUzIlttUvzhs3tdwDLwj7j5WPgQ3galoC7mK");
        TwitterFactory tf = new TwitterFactory(cfg.build());
        return tf.getInstance();
    }

    public static QueryResult findTweetsAboutReferendum(Twitter twitter) throws TwitterException {
        Query query = new Query("#referendum");
        query.setSince("2016-09-01");
        query.setUntil("2016-12-05");

        QueryResult result = twitter.search(query);
        if (result.getTweets() == null || result.getTweets().isEmpty()) {
            return null;
        }
        return result;
    }

    public static String fromNameToTwitterScreenName(String name) throws InterruptedException {

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

    public static long fromNameToTwitterID(String name) throws InterruptedException {
        long id = 0;
        Twitter twitter = TwitterUtils.getTwitter();
        boolean done = true;
        do {
            try {
                User user = twitter.searchUsers(name, 0).get(0);
                id = user.getId();

            } catch (TwitterException ex) {
                Logger.getLogger(PoliticiansLoader.class.getName()).log(Level.SEVERE, null, ex);
                TimeUnit.MINUTES.sleep(3);
                done = false;
            } catch (java.lang.IndexOutOfBoundsException e) {
                id = 0;
            }
        } while (!done);

        return id;
    }

}
