import Analysis.KcoreAndCC;
import Analysis.TemporalAnalysis;
import java.io.IOException;
import java.text.ParseException;
import twitter4j.JSONException;

public class Main {
    public static void main(String []args) throws IOException, JSONException, ParseException, Exception {
        boolean useCache = true;
        TemporalAnalysis.clusterTopNTerms(useCache, 1000, 12, 10);
        KcoreAndCC.extractKCoreAndConnectedComponent();
        
    }
}
