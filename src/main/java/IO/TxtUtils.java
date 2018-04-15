package IO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class TxtUtils {

    public static List<String> txtToArray(String filename) throws IOException {
        // read the file and return an array that each entry is a politician twitter name
        BufferedReader abc = new BufferedReader(new FileReader(filename));
        List<String> data = new ArrayList<>();
        String s;
        while ((s = abc.readLine()) != null) {
            data.add(s);
        }
        abc.close();
        return data;
    }
    
    public static void listToTxt(String filePath, List<String> arr) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        for (String str : arr) {
            writer.write(str);
            writer.write("\n");
        }
        writer.close();
    }
}
