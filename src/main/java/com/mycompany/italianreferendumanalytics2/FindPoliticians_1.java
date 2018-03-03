/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.italianreferendumanalytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author gabme
 */
public class FindPoliticians_1 {    
    private static int countNo;
    private static int countYes;
    private static final int FOLLOWERSTHRESHOLD = 20000;
    
    
    private static Twitter getTwitter(){
        ConfigurationBuilder cfg = new ConfigurationBuilder();
        cfg.setOAuthAccessToken("804265252221308928-QMQRe5XZPRSBmXqZTYD1ESzOoiNDY7y");
        cfg.setOAuthAccessTokenSecret("ykK4tbpXus9Yggh41ChMnbct4mHjbc0gDxEcyOYerX9SD");
        cfg.setOAuthConsumerKey("42Q1oVVQ8wDmWRPAk8tx0lMRk");
        cfg.setOAuthConsumerSecret("MIjyDlLibLTwpZgUzIlttUvzhs3tdwDLwj7j5WPgQ3galoC7mK");
        TwitterFactory tf = new TwitterFactory(cfg.build());
        return tf.getInstance();
    }
    
    //check if the user has any political role written in the description
    //of the twitter account
    private static boolean isPolitician(User user){
        String[] politicalOfficeKeywords = {
            "deputato", "deputata", "senatore", "senatrice", "ministro", 
            "sindaco", "politico", "politica", "candidato", "candidata",
            "senato", "deputati"};
        return Arrays.stream(politicalOfficeKeywords).anyMatch(user.getDescription()::contains);
    }
    
    //Check among the friends of <username> who is a politician. Return the list of those
    private static List<User> findPoliticiansAmongFriends(Twitter twitter, String username) throws TwitterException{        
        List<User> politicians= new ArrayList<User>();
        
        long lCursor = -1;
        List<User> friends = twitter.getFriendsList(username, lCursor);
        for (User user : friends)
        {
            if(isPolitician(user)){
                //check if it is a relevant politician
                if(user.getFollowersCount() > FOLLOWERSTHRESHOLD){
                    politicians.add(user);
                }
            }
        }
        return politicians;
    }
    
    private static QueryResult findTweetsAboutReferendum(Twitter twitter) throws TwitterException{
        Query query = new Query("#referendum");
        query.setSince("2016-09-01");
        query.setUntil("2016-12-05");
        
        QueryResult result = twitter.search(query);
        if(result.getTweets() == null || result.getTweets().isEmpty()){
            return null;
        }
        return result;
    }
    
    
    public static void main(String []args) throws TwitterException{
        Twitter twitter = getTwitter();
        List<User> politicians = findPoliticiansAmongFriends(twitter, "pdnetwork");
        QueryResult result = findTweetsAboutReferendum(twitter);
    }
}
