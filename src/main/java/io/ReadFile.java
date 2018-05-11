/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io;
// source: http://www.technical-recipes.com/2011/reading-text-files-into-string-arrays-in-java/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadFile {

    public String[] readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);

        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;

        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }

        bufferedReader.close();

        return lines.toArray(new String[lines.size()]);
    }

    public static File[] findFilesInDirectory(String directoryPath) {
        // Finds all files and folders in a path and returns them
        File folder = new File(directoryPath);

        File[] files = folder.listFiles();
        return files;
    }
}
