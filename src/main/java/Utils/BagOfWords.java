package Utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

public class BagOfWords {
    private HashMap<String, Integer> bagOfWords;
    private boolean removeStopwords;
    private Set<String> stopwords;
    
    public BagOfWords(boolean removeStopwords) throws FileNotFoundException, IOException{
        this.bagOfWords = new HashMap<>();
        this.removeStopwords = removeStopwords;
        
        //load the stopwords
        if(removeStopwords){
            this.stopwords = new HashSet<>();
            loadStopwords();
        }
    }
    
    public boolean getRemoveStopwords(){
        return this.removeStopwords;
    }
    
    public void setRemoveStopwords(boolean removeStopwords){
        this.removeStopwords = removeStopwords;
    }
    
    public HashMap<String, Integer> getBagOfWords(){
        return this.bagOfWords;
    }
    
    public void loadStopwords() throws FileNotFoundException, IOException{
        FileInputStream stream = new FileInputStream("src/main/resources/stopwords.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String stopword;
        while((stopword = br.readLine()) != null) {
            this.stopwords.add(stopword);
        }
    }
    
    public void add(String term){
        if("".equals(term) || (this.removeStopwords && this.stopwords.contains(term)))
            return;
        
        if(this.bagOfWords.containsKey(term)){
            this.bagOfWords.put(term, this.bagOfWords.get(term) + 1);
        }
        else this.bagOfWords.put(term, 1);
    }
    
    public void add(String[] terms) {
        for(String term : terms){
            add(term);
        }
    }
    
    //reference: https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/
    private HashMap<String, Integer> sortByValues() { 
       List list = new LinkedList(this.bagOfWords.entrySet());
       // Defined Custom Comparator here
       Collections.sort(list, (Object o1, Object o2) -> ((Comparable) ((Map.Entry) (o2)).getValue())
               .compareTo(((Map.Entry) (o1)).getValue()));

       HashMap sortedHashMap = new LinkedHashMap();
       for (Iterator it = list.iterator(); it.hasNext();) {
              Map.Entry entry = (Map.Entry) it.next();
              sortedHashMap.put(entry.getKey(), entry.getValue());
       } 
       return sortedHashMap;
    }
    
    public List<String> getFirstNTerms(int N){
        HashMap<String, Integer> sortedBagOfWords = sortByValues();
        List<String> firstNTerms = new ArrayList<>();
        
        if(N >= sortedBagOfWords.size()){
            firstNTerms.addAll(sortedBagOfWords.keySet());
            return firstNTerms;
        } 
        
        int i = 0;
        for(String term : sortedBagOfWords.keySet()){
            if(i >= N) break;
            firstNTerms.add(term);
            i++;
        }
        return firstNTerms;
    }
}
