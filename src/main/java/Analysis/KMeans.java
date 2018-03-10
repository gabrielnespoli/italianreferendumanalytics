package Analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.commons.text.similarity.HammingDistance;

public class KMeans {
    private final HammingDistance distanceCalculator;
    
    public KMeans(HammingDistance distanceCalculator){
        this.distanceCalculator = distanceCalculator;
    }
    
    // get a random centroid in the space vector of SAX strings
    // the centroid will have the length of saxStringSize and composed by characters from 'a' to lastAlphabetLetter
    private String getCentroid(char maxAlphabetLetter, int saxStringSize){
        RandomStringGenerator randomStringGenerator =
        new RandomStringGenerator.Builder()
                .withinRange('a', maxAlphabetLetter)
                .filteredBy(CharacterPredicates.LETTERS, CharacterPredicates.DIGITS)
                .build();
        return randomStringGenerator.generate(saxStringSize);
    }
    
    // return an array of the same size of items which in each position is stored the cluster of that item
    public int[] getClusterIDs(int k, HashMap<String, String> hmSaxStrings){
        ArrayList<String> listItems = new ArrayList<>(hmSaxStrings.values()); //"convert" the collection to arraylist
        int listSize = listItems.size();
        int[] arrayClusterID = new int[listSize];

        // initialize the centroids
        String[] centroids = new String[k];
        for(int i=0; i<k; i++)
            centroids[i] = getCentroid( (char) listItems.get(0).chars().max().getAsInt(), listItems.get(0).length());
        
        // iterate through all items
        for(int i=0; i<listSize; i++){
            Integer minDistance = Integer.MAX_VALUE;
            String item = listItems.get(i);
            // iterate through all centroids
            int newDist;
            for(int j=0; j<k; j++){
                newDist = distanceCalculator.apply(item, centroids[j]);
                if(newDist < minDistance) {
                    minDistance = newDist;
                    arrayClusterID[i] = j;
                }
            }
        }
        return arrayClusterID;
    }
    
    // returns a list of LinkedHashSet, in which in each position of the list is 
    // a cluster (LinkedHashSet) of the similar terms (strings)
    public ArrayList<LinkedHashSet<String>> getClusterOfTerms(int k, HashMap<String, String> hmSaxStrings){
        int[] arrayClusterID = this.getClusterIDs(k, hmSaxStrings);
        
        ArrayList<String> saxStrings = new ArrayList<>(hmSaxStrings.values()); //"convert" the collection of saxstrings to arraylist
        
        // create a list with the terms/words to allow indexed access
        ArrayList<String> terms = new ArrayList<>();
        hmSaxStrings.keySet().forEach((term) -> {
            terms.add(term);
        });
        
        //instantiate the sets inside the list
        ArrayList<LinkedHashSet<String>> clusterOfTerms = new ArrayList<>();
        for(int i=0; i<k;i++)
            clusterOfTerms.add(new LinkedHashSet<>());
        
        //iterate through the cluster IDs adding creating the set of similar terms of each cluster ID
        for(int i=0; i<arrayClusterID.length; i++){
            int clusterID = arrayClusterID[i];                              //get the cluster ID in the position i
            LinkedHashSet<String> cluster = clusterOfTerms.get(clusterID);  //get the set of terms of clusterID
            cluster.add(terms.get(i));                                      //and add the term in position i
        }
        
        return clusterOfTerms;
    }
}
