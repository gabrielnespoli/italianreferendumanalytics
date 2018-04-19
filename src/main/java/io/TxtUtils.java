package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class TxtUtils {

    public static List<String> txtToList(String filename) throws IOException {
        // read the file and return an array that each entry is a politician twitter name
        BufferedReader br = new BufferedReader(new FileReader(filename));
        List<String> data = new ArrayList<>();
        String s;
        while ((s = br.readLine()) != null) {
            data.add(s);
        }
        br.close();
        return data;
    }

    public static LinkedHashSet txtToSet(String filename) throws IOException {
        // read the file and return an array that each entry is a politician twitter name
        BufferedReader br = new BufferedReader(new FileReader(filename));
        LinkedHashSet data = new LinkedHashSet<>();
        String s;
        while ((s = br.readLine()) != null) {
            // if the data read is an integer, convert the string to int
            if (s.matches("\\d+")) {
                data.add(new Integer(s));
            } else {
                data.add(s);
            }
        }
        br.close();
        return data;
    }

    public static void iterableToTxt(String filePath, Iterable iter) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        Iterator iterator = iter.iterator();
        Object obj;
        while (iterator.hasNext()) {
            obj = iterator.next();
            writer.write(obj.toString());
            writer.write("\n");
        }
        writer.close();
    }
}
