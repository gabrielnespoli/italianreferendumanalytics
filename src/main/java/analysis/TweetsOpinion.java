package analysis;

import java.io.File;
import java.io.IOException;
import io.TxtUtils;
import index.IndexBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import static org.apache.lucene.util.Version.LUCENE_41;
import twitter4j.JSONException;

public class TweetsOpinion {
    
    public static int showProgress(int counter, int maxLen){
        float p = (float) counter/maxLen;
        System.out.println(p * 100.0 + " % done");
        return counter + 1;
    }

    private static void findSupporters(String filenamePoliticians, String filenameOutput, String M) throws IOException, ParseException {
        // politicians input, user <-> politician output, set of users M
        // Load the all tweets index and analyzer
        ItalianAnalyzer analyzer = new ItalianAnalyzer(Version.LUCENE_41, IndexBuilder.STOPWORDS);
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_41, analyzer);

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

            Query q = parser.parse("+ " + politician);

            // Find tweets. I put a very high threshold so i find all of them
            TopDocs top = searcher.search(q, 10000000);
            ScoreDoc[] hits = top.scoreDocs;

            // Go on each hit
            Document doc = null;

            String s = "";

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
        TxtUtils.iterableToTxt(filenameOutput, saveHits);

        // Show the asked data
        Set uniqueUsers = new HashSet(users);
        System.out.println("Unique users: " + uniqueUsers.size());
        System.out.println("Total tweets: " + saveHits.size());

        // save the set M
        List<String> uniqueUsersList = new ArrayList<>();
        uniqueUsersList.addAll(uniqueUsers);
        TxtUtils.iterableToTxt(M, uniqueUsersList);
    }

    public static void main(String[] args) throws IOException, JSONException, ParseException, java.text.ParseException {
        // Uncomment to create the inverted index of all tweets
        //createIndexAllTweets();
        System.out.println("#### YES");
        findSupporters("src/main/resources/yes_politicians.txt", "src/main/resources/yes_users_mention_politicians.txt", "src/main/resources/yes_M.txt");

        System.out.println("#### NO");
        findSupporters("src/main/resources/no_politicians.txt", "src/main/resources/no_users_mention_politicians.txt", "src/main/resources/no_M.txt");
    }
}
