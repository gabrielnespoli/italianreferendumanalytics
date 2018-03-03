
import Utils.GzipReader;
import Utils.Parser;
import java.io.BufferedReader;
import java.io.IOException;
import twitter4j.JSONException;
import twitter4j.JSONObject;


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
        String xml = br.readLine();
        JSONObject json = Parser.getJSON(xml);
        System.out.println(xml);
    }
}
