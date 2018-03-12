import Analysis.TemporalAnalysis;
import java.io.IOException;
import java.text.ParseException;
import twitter4j.JSONException;

public class Main {
    public static void main(String []args) throws IOException, JSONException, ParseException, Exception {
        
        TemporalAnalysis.clusterTopNTerms(true, 1000, 12);
        String filename = "";
        
    }
}
