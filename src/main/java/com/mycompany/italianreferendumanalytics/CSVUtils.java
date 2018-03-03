/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.italianreferendumanalytics;

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

public class CSVUtils {
    private static String INDEX_DIRECTORY = "src/main/resources/";
    private static String csvFile = "politicians.csv";
    private static String sep = ";";
    
    public static void setDirectory(String directory) {
        INDEX_DIRECTORY = directory;
    }
        
    public static void setFilename(String filename){
        csvFile = filename;
    }

    public static void setSeparator(String separator){
        sep = separator;
    }
    
    public static String getDirectory(){
        return INDEX_DIRECTORY;
    }
    
    public static String Filename(){
        return csvFile;
    }
    
    public static String getSeparator(){
        return sep;
    }
    
    public static List<String[]> readCSV(){
        String line;
        List<String[]> politicians = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(INDEX_DIRECTORY+csvFile))) {
            while ((line = br.readLine()) != null) {
                politicians.add(line.split(sep));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return politicians;
    }
    
    public static void writeCSV(List<String[]> politicians) throws IOException{
        PrintWriter pw = new PrintWriter(new FileWriter(INDEX_DIRECTORY+"politicians_loaded.csv"));
        politicians.forEach((politicianLine) -> {
            pw.write(politicianLine[0] + ";");
            if(politicianLine.length > 1) {
                pw.write(politicianLine[1]);
            }
            pw.write("\n");
        });
        pw.close();
    }
}