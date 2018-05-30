package analysis;

import static analysis.GraphAnalysis.RESOURCES_LOCATION;
import static analysis.GraphAnalysis.runner;
import gnu.trove.map.TIntLongMap;
import java.io.File;
import java.io.IOException;
import io.TxtUtils;
import index.IndexBuilder;
import static io.TxtUtils.TxtToMap;
import static io.TxtUtils.mapToTxt;
import it.stilo.g.algo.HubnessAuthority;
import it.stilo.g.structures.DoubleValues;
import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.structures.WeightedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import static org.apache.lucene.util.Version.LUCENE_41;
import structure.MappedWeightedGraph;
import twitter4j.JSONException;

public class TweetsOpinion {

    public static int showProgress(int counter, int maxLen) {
        float p = (float) counter / maxLen;
        System.out.println(p * 100.0 + " % done");
        return counter + 1;
    }

    public static HashSet txtToSetString(String filename) throws IOException {
        // read the file and return an array that each entry is a politician twitter name
        BufferedReader br = new BufferedReader(new FileReader(filename));
        HashSet data = new HashSet();
        String s;
        while ((s = br.readLine()) != null) {

            data.add(s);

        }
        br.close();
        return data;
    }

    private static void findSupporters(String filenamePoliticians, String filenameOutput, String mapfilenameOutput) throws IOException, ParseException {
        // politicians input, user <-> politician output
        // this is for part 1.3 to create the graph
        // Load the all tweets index and analyzer
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41, IndexBuilder.STOPWORDS);

        File directoryIndex = new File(IndexBuilder.INDEX_DIRECTORY + "all_tweets_index/");
        IndexReader ir = DirectoryReader.open(FSDirectory.open(directoryIndex));
        IndexSearcher searcher = new IndexSearcher(ir);
        QueryParser parser = new QueryParser(LUCENE_41, "term", analyzer);

        // Get the list of politicians
        List<String> listPoliticians = TxtUtils.txtToList(filenamePoliticians);

        // iterate over politicians to find them in the tweets
        // Save the results using this list
        List<String> saveHits = new ArrayList<>();
        //Unique users to show statistics
        List<String> users = new ArrayList<>();

        int counter = 0;

        for (String politician : listPoliticians) {
            counter = showProgress(counter, listPoliticians.size());

            Query q = parser.parse(politician); // removed the +

            // Find tweets. I put a very high threshold so i find all of them
            TopDocs top = searcher.search(q, 10000000);
            ScoreDoc[] hits = top.scoreDocs;

            Document doc;
            String s;

            // go on each hit
            for (ScoreDoc entry : hits) {
                doc = searcher.doc(entry.doc);
                //System.out.println("field1: " + doc.get("user"));
                s = doc.get("user") + " " + politician;
                saveHits.add(s);
                users.add(doc.get("user"));
            }
        }
        // Save to file as <user> <polititian>
        Map<String, Integer> map = new HashMap<>();
        for (String row : saveHits) {
            counter = showProgress(counter, saveHits.size());
            // add the user to the counter and add one to the counter
            map.compute(row, (key, oldValue) -> ((oldValue == null) ? 1 : oldValue + 1));
        }

        TxtUtils.iterableToTxt(filenameOutput, saveHits);
        mapToTxt(mapfilenameOutput, map);

        // Show the asked data
        //Set uniqueUsers = new HashSet(users);
        //System.out.println("Unique users: " + uniqueUsers.size());
        //System.out.println("Total tweets: " + saveHits.size());
        // save the set M
        //List<String> uniqueUsersList = new ArrayList<>();
        //uniqueUsersList.addAll(uniqueUsers);
        //TxtUtils.iterableToTxt(M, uniqueUsersList);
    }

    private static Map<String, Integer> classifySupporters(String mode, String filepath) throws IOException, ParseException {
        // finds the tweets that contain either the politicians (yes or no), or the word, provided in the filepath
        // store here the query
        String queryTxt = "";
        // Load the list of politicians
        System.out.println("tokens in the query: ");
        if ("politicians".equals(mode)) {
            List<String> politicians = TxtUtils.txtToList(filepath);

            for (String p : politicians) {
                queryTxt += " " + p;
            }
            System.out.print(politicians.size());
        }

        // Load the list of yes words
        if ("words".equals(mode)) {
            List<String> words = TxtUtils.txtToList(filepath);

            Set<String> wordsClean = new HashSet<>();

            for (String w : words) {
                wordsClean.add(w.split(" ")[0]);
                wordsClean.add(w.split(" ")[1]);
            }

            for (String w : wordsClean) {
                queryTxt += " " + w;
            }
            System.out.print(wordsClean.size());
        }

        //System.out.println(queryTxtWords);
        // Load the all tweets index and analyzer
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41, IndexBuilder.STOPWORDS);

        File directoryIndex = new File(IndexBuilder.INDEX_DIRECTORY + "all_tweets_index/");
        IndexReader ir = DirectoryReader.open(FSDirectory.open(directoryIndex));
        IndexSearcher searcher = new IndexSearcher(ir);
        QueryParser parser = new QueryParser(LUCENE_41, "term", analyzer);

        Query q = parser.parse(queryTxt);
        // Find tweets. I put a very high threshold so i find all of them
        TopDocs top = searcher.search(q, 100000000);
        ScoreDoc[] hits = top.scoreDocs;
        System.out.println(hits.length);

        // i create a dict where the keys are the usernames and the values the number of times the
        // user apears in a document result as the writer
        Map<String, Integer> map = new HashMap<>();
        String user;
        Document doc;
        // Go to each doc
        int counter = 0;
        for (ScoreDoc entry : hits) {
            counter = showProgress(counter, hits.length);
            doc = searcher.doc(entry.doc);
            user = doc.get("user");
            // add the user to the counter and add one to the counter
            map.compute(user, (key, oldValue) -> ((oldValue == null) ? 1 : oldValue + 1));
        }
        return map;
    }

    private static void makeYesNoScores(Map<String, Integer> mapYesPoliticians, Map<String, Integer> mapNoPoliticians, Map<String, Integer> mapYesWords, Map<String, Integer> mapNoWords) throws IOException {
        // this divides the users that mention the politicians into yes or no  suporters

        // create the bag of all users
        Set<String> allUsers = new HashSet();
        Set<String> tmpYes = mapYesPoliticians.keySet();
        Set<String> tmpNo = mapNoPoliticians.keySet();
        allUsers.addAll(tmpYes);
        allUsers.addAll(tmpNo);

        // create sets of users yes and no (M)
        Set<String> mYes = new HashSet();
        Set<String> mNo = new HashSet();

        // iterate over all users and compute the score
        int counter = 0;
        for (String user : allUsers) {
            counter = showProgress(counter, allUsers.size());
            int yesScoreP;
            int noScoreP;
            int yesScoreW;
            int noScoreW;
            yesScoreP = mapYesPoliticians.getOrDefault(user, 0);
            noScoreP = mapNoPoliticians.getOrDefault(user, 0);
            yesScoreW = mapYesWords.getOrDefault(user, 0);
            noScoreW = mapNoWords.getOrDefault(user, 0);
            int finalScoreP = yesScoreP - noScoreP;
            int finalScoreW = yesScoreW - noScoreW;

            System.out.println("User: " + user + ", W score: " + finalScoreW);

            if (finalScoreP > 0 && finalScoreW > 0) {
                mYes.add(user);
            }
            if (finalScoreP > 0 && finalScoreW < 0) {
                mNo.add(user);
            }
            if (finalScoreP < 0 && finalScoreW < 0) {
                mNo.add(user);
            }
            if (finalScoreP < 0 && finalScoreW > 0) {
                mYes.add(user);
            }
        }
        TxtUtils.iterableToTxt("src/main/resources/yes_M.txt", mYes);
        TxtUtils.iterableToTxt("src/main/resources/no_M.txt", mNo);
    }

    public static void hubnessGraph13() throws IOException {
        // Load the key = <user> <politician>, value = <number of times mentioned>
        Map<String, Integer> mapYesPoliticians = new HashMap<String, Integer>();
        mapYesPoliticians = TxtToMap("src/main/resources/yes_map_users_mention_politicians.txt");
        Map<String, Integer> mapNoPoliticians = new HashMap<String, Integer>();
        mapNoPoliticians = TxtToMap("src/main/resources/no_map_users_mention_politicians.txt");

        // Create the lists of nodes and weights
        List<Integer> users = new ArrayList<>();
        List<Integer> politicians = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        // Create the mapper to get back the usernames
        NodesMapper<String> mapper = new NodesMapper<>();

        // Go on each <user> <politician> and get the weight and put all in the list
        for (String userPol : mapYesPoliticians.keySet()) {
            String user = userPol.split(" ")[0];
            String politician = userPol.split(" ")[1];
            users.add(mapper.getId(user));
            politicians.add(mapper.getId(politician));
            weights.add(mapYesPoliticians.get(userPol));
        }

        for (String userPol : mapNoPoliticians.keySet()) {
            String user = userPol.split(" ")[0];
            String politician = userPol.split(" ")[1];
            users.add(mapper.getId(user) - 1);
            politicians.add(mapper.getId(politician) - 1);
            weights.add(mapNoPoliticians.get(userPol));
        }

        // Convert lists to arrays to use Stilo library
        int[] usersArray = users.stream().mapToInt(i -> i).toArray();
        int[] politiciansArray = politicians.stream().mapToInt(i -> i).toArray();
        int[] weightsArray = weights.stream().mapToInt(i -> i).toArray();

        // Create the graph
        WeightedDirectedGraph g = new WeightedDirectedGraph(usersArray.length + 1);
        int counter = 0;
        System.out.println("Creating graph...");
        for (int i = 0; i < usersArray.length; i++) {
            counter = showProgress(counter, usersArray.length);
            g.add(usersArray[i], politiciansArray[i], weightsArray[i]);
        }

        // Calculate hubness authority
        System.out.println("Calculating hubness and authority");
        int worker = (int) (Runtime.getRuntime().availableProcessors());
        ArrayList<ArrayList<DoubleValues>> list;
        list = HubnessAuthority.compute(g, 0.00001, worker);

        // Store in dicts the hubness and authority scores
        Map<String, Double> authScore = new HashMap<>();
        Map<String, Double> hubScore = new HashMap<>();

        for (int i = 0; i < list.size(); i++) {
            ArrayList<DoubleValues> score = list.get(i);
            String x = "";
            if (i == 0) {
                x = "Auth ";
                for (int j = 0; j < score.size(); j++) {
                    authScore.put(mapper.getNode(score.get(j).index + 1), score.get(j).value);
                }
            } else {
                x = "Hub ";
                for (int j = 0; j < score.size(); j++) {
                    hubScore.put(mapper.getNode(score.get(j).index + 1), score.get(j).value);
                }
            }
        }

        // Define a final measure of centrality, the mean of both scores in negative (less is more central)
        // i set this definition to user sort later
        System.out.println("Calculating final score");
        Map<String, Double> hubnessAuthority = new HashMap<String, Double>();
        for (String user : hubScore.keySet()) {
            double score = -(authScore.get(user) + hubScore.get(user)) / 2.0;
            hubnessAuthority.put(user, score);
            //System.out.println(user + " total score: " + score + "( auth score: " + authScore.get(user) + ", hub score: "+ hubScore.get(user) + ")");
        }

        // Sort the users by value of the map
        Map<String, Double> hubnessAuthoritySorted = new HashMap<String, Double>();
        hubnessAuthoritySorted = sortByValue(hubnessAuthority);

        // Import the yes and no users
        List<String> usersYes;
        usersYes = TxtUtils.txtToList("src/main/resources/yes_M.txt");

        List<String> usersNo;
        usersNo = TxtUtils.txtToList("src/main/resources/no_M.txt");

        // Store here the top 500
        List<String> usersYes500 = new ArrayList<>();
        List<String> usersNo500 = new ArrayList<>();

        // Iterate over the top of the list, and add the users to the top 500 yes/no supporters
        int counterYes = 0;
        int counterNo = 0;
        for (String user : hubnessAuthoritySorted.keySet()) {
            System.out.println(user + " " + hubnessAuthoritySorted.get(user) + " ( auth score: " + authScore.get(user) + ", hub score: " + hubScore.get(user) + ")");
            if (usersYes.contains(user) & counterYes < 500) {
                usersYes500.add(user);
                counterYes++;
            }
            if (usersNo.contains(user) & counterNo < 500) {
                usersNo500.add(user);
                counterNo++;
            }

            // Stop iterating
            if (counterYes == 500 & counterNo == 500) {
                break;
            }
        }

        System.out.println(usersYes500.size() + ", " + usersNo500.size());
        TxtUtils.iterableToTxt("src/main/resources/yes_top500_hub_auth.txt", usersYes500);
        TxtUtils.iterableToTxt("src/main/resources/no_top500_hub_auth.txt", usersNo500);

    }

    // source stackoverflow
    private static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return ((Comparable<V>) ((Map.Entry<K, V>) (o1)).getValue()).compareTo(((Map.Entry<K, V>) (o2)).getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Iterator<Entry<K, V>> it = list.iterator(); it.hasNext();) {
            Map.Entry<K, V> entry = (Map.Entry<K, V>) it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static void saveTop500HubnessAuthorities(MappedWeightedGraph gmap, LinkedHashSet<Integer> users, LongIntDict mapLong2Int, int minNumberComments) throws InterruptedException, IOException, ParseException {
        WeightedGraph g = gmap.getWeightedGraph();
        TIntLongMap mapInt2Long = mapLong2Int.getInverted();

        // get the authorities
        ArrayList<ArrayList<DoubleValues>> authorities = HubnessAuthority.compute(g, 0.00001, runner);
        ArrayList<DoubleValues> auth = authorities.get(0);
        ArrayList<DoubleValues> hub = authorities.get(1);

        Map<Long, Double> mapLongAuth = new HashMap<>();
        Map<Long, Double> mapLongHub = new HashMap<>();
        for (DoubleValues score : auth) {
            mapLongAuth.put(mapInt2Long.get(score.index), score.value);   // map back the ids in 'score' to the previous id, before the resizing, then map back to the twitter ID
        }
        for (DoubleValues score : hub) {
            mapLongHub.put(mapInt2Long.get(score.index), score.value);
        }

        // get the users that mention the politicians >= than minNumberComments times
        List<String> yesUserMentionPolitician = TxtUtils.txtToList(RESOURCES_LOCATION + "yes_map_users_politicians.txt");
        Map<Long, Integer> userIdCountYesPol = new HashMap<>();
        for (String row : yesUserMentionPolitician) {
            String username = row.split("=")[0];  // get the user screen name
            Document[] docs = index.IndexSearcher.searchByField("all_tweets_index/", "user", username, 1);

            if (docs != null) {
                Long id = Long.parseLong(docs[0].get("id"));  //read just the first resulting doc
                int ths = Integer.parseInt(row.split("=")[1]);
                if (ths >= minNumberComments) {
                    userIdCountYesPol.put(id, ths);  // retrieve the twitter ID (long) and covert to int

                }
            }
        }
        //  same thing for no users
        List<String> noUserMentionPolitician = TxtUtils.txtToList(RESOURCES_LOCATION + "no_map_users_politicians.txt");
        Map<Long, Integer> userIdCountNoPol = new HashMap<>();
        for (String row : noUserMentionPolitician) {
            String username = row.split("=")[0];  // get the user screen name
            Document[] docs = index.IndexSearcher.searchByField("all_tweets_index/", "user", username, 1);

            if (docs != null) {
                Long id = Long.parseLong(docs[0].get("id"));  //read just the first resulting doc
                int ths = Integer.parseInt(row.split("=")[1]);
                if (ths >= minNumberComments) {
                    userIdCountNoPol.put(id, ths);  // retrieve the twitter ID (long)

                }
            }
        }

        // now split them using the M files
        List<String> yesUsers = TxtUtils.txtToList(RESOURCES_LOCATION + "yes_M_IDs.txt");
        List<String> noUsers = TxtUtils.txtToList(RESOURCES_LOCATION + "no_M_IDs.txt");
        List<Long> noUsersFinal = new ArrayList<>();
        List<Long> yesUsersFinal = new ArrayList<>();

        for (String u : yesUsers) {
            Long ul = Long.parseLong(u);
            if (userIdCountNoPol.containsKey(ul) || userIdCountYesPol.containsKey(ul)) {
                yesUsersFinal.add(ul);
            }
        }

        for (String u : noUsers) {
            Long ul = Long.parseLong(u);
            if (userIdCountNoPol.containsKey(ul) || userIdCountYesPol.containsKey(ul)) {
                noUsersFinal.add(ul);
            }
        }

        System.out.println("Calculating final score");
        Map<Long, Double> hubnessAuthority = new HashMap<>();
        for (Long user : mapLongHub.keySet()) {
            double sc = -(mapLongHub.get(user) + mapLongAuth.get(user)) / 2.0;
            if (userIdCountNoPol.containsKey(user) || userIdCountYesPol.containsKey(user)) {
                hubnessAuthority.put(user, sc);
            }
        }

        Map<Long, Double> hubnessAuthoritySorted = new HashMap<>();
        hubnessAuthoritySorted = sortByValue(hubnessAuthority);

        // Now get the top users for each set
        int counterYes = 0;
        int counterNo = 0;
        ArrayList<Long> ahYes = new ArrayList<>();
        ArrayList<Long> ahNo = new ArrayList<>();
        for (Long user : hubnessAuthoritySorted.keySet()) {
            if (yesUsersFinal.contains(user) && counterYes < 500) {
                ahYes.add(user);
                counterYes++;
            }
            if (noUsersFinal.contains(user) && counterNo < 500) {
                ahNo.add(user);
                counterNo++;
            }
        }

        // save the first topk authorities
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "top_hubness_authorities_yes.txt", ahYes);
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "top_hubness_authorities_no.txt", ahNo);
    }

    public static void buildMGroup(boolean useCache) throws IOException, JSONException, ParseException, java.text.ParseException {
        // Uncomment to create the inverted index of all tweets
        //createIndexAllTweets();

        Map<String, Integer> mapYesPoliticians;
        Map<String, Integer> mapYesWords;
        Map<String, Integer> mapNoPoliticians;
        Map<String, Integer> mapNoWords;

        // Part 1.1
        // Find the number of times the users mention the politicians or the words of the clusters, this has to be ran only once
        if (!useCache) {
            mapYesPoliticians = classifySupporters("politicians", "src/main/resources/yes_politicians.txt");
            mapYesWords = classifySupporters("words", "src/main/resources/yes_kcore.txt");

            mapToTxt("src/main/resources/yes_map_users_politicians.txt", mapYesPoliticians);
            mapToTxt("src/main/resources/yes_map_users_words.txt", mapYesWords);

            mapNoPoliticians = classifySupporters("politicians", "src/main/resources/no_politicians.txt");
            mapNoWords = classifySupporters("words", "src/main/resources/no_kcore.txt");
            mapToTxt("src/main/resources/no_map_users_politicians.txt", mapNoPoliticians);
            mapToTxt("src/main/resources/no_map_users_words.txt", mapNoWords);
        }

        mapYesPoliticians = TxtToMap("src/main/resources/yes_map_users_politicians.txt");
        mapNoPoliticians = TxtToMap("src/main/resources/no_map_users_politicians.txt");
        mapYesWords = TxtToMap("src/main/resources/yes_map_users_words.txt");
        mapNoWords = TxtToMap("src/main/resources/no_map_users_words.txt");

        // Make the groups
        makeYesNoScores(mapYesPoliticians, mapNoPoliticians, mapYesWords, mapNoWords);

        // Part 1.1 and 1.3, this creates a file where each line is a tweet: <user> <politician>, and can be repeated
        // and an other file that is <user> <politician> <number of times mentioned>
        // Uncomment to create the list of users who mention yes or no politicians
        //System.out.println("#### YES");
        findSupporters("src/main/resources/yes_politicians.txt", "src/main/resources/yes_users_mention_politicians.txt", "src/main/resources/yes_map_users_mention_politicians.txt");

        //System.out.println("#### NO");
        findSupporters("src/main/resources/no_politicians.txt", "src/main/resources/no_users_mention_politicians.txt", "src/main/resources/no_map_users_mention_politicians.txt");

        // Part 1.3 
        //hubnessGraph13();
    }
}
