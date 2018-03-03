
import Utils.GzipReader;
import Utils.Parser;
import java.io.BufferedReader;
import java.io.IOException;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import Utils.BagOfWords;

public class Main {
    public static void main(String []args) throws IOException, JSONException {
        String filename = "";
        
        // it means that the user has provided a filename
        if(args.length != 0) {
            filename = args[0];
        }
        else {
            filename = "src/main/resources/sbn-data/stream/day-1480170614348/TW-1480170614348.gz";
        }
        BufferedReader br = GzipReader.getBufferedReaderGzFile(filename);
        String tweet;
        JSONObject json;
        
        BagOfWords bw = new BagOfWords(true);
        int i = 0;
        while ((tweet = br.readLine()) != null) {
            json = Parser.getJSON(tweet);
            tweet = (String) json.get("text");
            tweet = tweet.toLowerCase();
            tweet = tweet.replaceAll("[^\\p{L}\\s]", ""); // remove the especial characters and punctuations
            bw.add(tweet.split(" "));
            
            System.out.println(i);
            i++;
        }
        
        bw.getFirstNTerms(1000).forEach((term) -> {
            System.out.println(term);
        });
    }
}
