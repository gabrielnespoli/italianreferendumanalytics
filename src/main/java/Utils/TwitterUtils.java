package Utils;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public abstract class TwitterUtils {

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
}
