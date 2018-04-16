package preprocess;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TextProcessor {

    private boolean removeStopWords;
    private Set<String> stopWords;

    public TextProcessor(boolean removeStopWords) throws FileNotFoundException, IOException {
        this.removeStopWords = removeStopWords;

        //load the stopwords
        if (removeStopWords) {
            this.stopWords = new HashSet<>();
            loadStopWords();
        }
    }

    private void loadStopWords() throws FileNotFoundException, IOException {
        FileInputStream stream = new FileInputStream("src/main/resources/stopwords.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String stopword;
        while ((stopword = br.readLine()) != null) {
            this.stopWords.add(stopword);
        }
    }

    public String process(String text) {
        text = text.toLowerCase();
        return text.replaceAll("[^\\p{L}\\s]", ""); // remove the especial characters and punctuations
    }

    public ArrayList<String> tokenize(String text) {
        ArrayList<String> tokens = new ArrayList<>(Arrays.asList(text.split(" ")));
        ArrayList<String> stopWords = new ArrayList<>();

        if (this.removeStopWords) {
            tokens.forEach((token) -> {
                if (this.stopWords.contains(token)) {
                    stopWords.add(token);
                }
            });
        }
        tokens.removeAll(stopWords);
        return tokens;
    }
}
