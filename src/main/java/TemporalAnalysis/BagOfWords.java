package TemporalAnalysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BagOfWords {
    private static HashMap<String, Integer> bagOfWords;
    
    public void BagOfWords(){
        bagOfWords = new HashMap<>();
    }
    
    public HashMap<String, Integer> getBagOfWords(){
        return bagOfWords;
    }
    
    public void add(String term){
        if(bagOfWords.containsKey(term)){
            bagOfWords.put(term, bagOfWords.get(term) + 1);
        }
        else bagOfWords.put(term, 1);
    }
    
    //reference: https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/
    private static HashMap<String, Integer> sortByValues() { 
       List list = new LinkedList(bagOfWords.entrySet());
       // Defined Custom Comparator here
       Collections.sort(list, (Object o1, Object o2) -> ((Comparable) ((Map.Entry) (o1)).getValue())
               .compareTo(((Map.Entry) (o2)).getValue()));

       HashMap sortedHashMap = new LinkedHashMap();
       for (Iterator it = list.iterator(); it.hasNext();) {
              Map.Entry entry = (Map.Entry) it.next();
              sortedHashMap.put(entry.getKey(), entry.getValue());
       } 
       return sortedHashMap;
    }
    
    public List<String> getFirstNKeys(){
    
    }
        
}
