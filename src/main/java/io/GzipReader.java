/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.nio.charset.StandardCharsets;

public class GzipReader {

    public static BufferedReader getBufferedReaderGzFile(String filename) throws IOException {
        // open the input (compressed) file.
        FileInputStream stream = new FileInputStream(filename);

        // open the gziped file to decompress.
        GZIPInputStream gzipstream = new GZIPInputStream(stream);
        Reader reader = new InputStreamReader(gzipstream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(reader);
        return br;
    }
}
