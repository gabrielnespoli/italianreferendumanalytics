package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/*
Reference: https://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/
 */
public abstract class CSVUtils {

    public static List<String[]> readCSV(String csvFile, String sep) {
        String line;
        List<String[]> politicians = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                politicians.add(line.split(sep));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return politicians;
    }

    public static void writeCSV(List<String[]> politicians, String destinationFile) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(destinationFile));
        politicians.forEach((politicianLine) -> {
            pw.write(politicianLine[0] + ";");
            if (politicianLine.length > 1) {
                pw.write(politicianLine[1]);
            }
            pw.write("\n");
        });
        pw.close();
    }
}
