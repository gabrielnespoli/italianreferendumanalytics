package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public abstract class TxtUtils {

    public static final String SEP = ",";

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

    /*
     * Read from a txt file and creates a list of the type defined by clazz, using reflection.
     * If more than one class is passed, the method will create a list of lists, will try to 
     * split the line by the Sep and each part will be added to a list. 
     * The method will then create a list of each value.
     * Ex: 
     *   txtToList(filename, Integer) returns ArrayList<Integer>
     *   txtToList(filename, Integer, Double) returns ArrayList<ArrayList<Object>>
     */
    public static ArrayList txtToList(String filename, Class... clazz) throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // read the file and return an array that each entry is a politician twitter name
        BufferedReader br = new BufferedReader(new FileReader(filename));
        ArrayList data = new ArrayList<>();
        ArrayList dataParts;
        String s;
        String[] parts;
        while ((s = br.readLine()) != null) {

            if (clazz.length > 1) {
                parts = s.split(SEP);
                dataParts = new ArrayList();
                for (int i = 0; i < parts.length; i++) {
                    dataParts.add(clazz[i].getConstructor(String.class).newInstance(parts[i]));
                }
                data.add(dataParts);
            } else {
                data.add(clazz[0].getConstructor(String.class).newInstance(s));
            }
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

    public static void mapToTxt(String filePath, Map<String, Integer> map) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        Object obj;
        while (iterator.hasNext()) {
            obj = iterator.next();
            writer.write(obj.toString());
            writer.write("\n");
        }
        writer.close();
    }

    public static Map<String, Integer> TxtToMap(String filePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        Map<String, Integer> data = new HashMap<String, Integer>();
        String s;
        while ((s = br.readLine()) != null) {
            data.put(s.split("=")[0], Integer.parseInt(s.split("=")[1]));
        }
        br.close();
        return data;
    }

}
