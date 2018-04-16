package analysis;

import utils.CharUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;

/*
A K-Means algorithm to cluster SAX strings.
*/
public class KMeans {

    private final HashMap<String, Integer> hmSaxCluster;
    private final int k;
    private final int alphabetSize;
    private final ArrayList<String> saxStrings;

    public KMeans(int k, int alphabetSize, ArrayList<String> saxStrings) {
        this.hmSaxCluster = new HashMap<>();
        this.k = k;
        this.alphabetSize = alphabetSize;
        this.saxStrings = saxStrings;
    }

    private String generateRandomCentroids() {
        char maxAlphabetLetter = (char) (97 + this.alphabetSize); //97 is the 'a' value in ASCII
        int saxStringSize = this.saxStrings.get(0).length();

        RandomStringGenerator randomStringGenerator
                = new RandomStringGenerator.Builder()
                        .withinRange('a', maxAlphabetLetter)
                        .filteredBy(CharacterPredicates.LETTERS, CharacterPredicates.DIGITS)
                        .build();
        return randomStringGenerator.generate(saxStringSize);
    }

    private String[] getRandomCentroids() {
        String[] centroids = new String[this.k];
        for (int i = 0; i < this.k; i++) {
            centroids[i] = generateRandomCentroids();
        }

        return centroids;
    }

    private String getCentroid(LinkedHashSet<String> cluster) {
        ArrayList<String> clusterList = new ArrayList<>(cluster);  // convert to list to allow indexed
        String centroid = "";

        // eventually after the first iteration, maybe there isn't any item in a
        // specific cluster (because a random generated centroid could have been 
        // bad localized). In this case, there isn't any update in the baricenter
        if (cluster.isEmpty()) {
            return generateRandomCentroids();
        }

        int saxStringSize = 0;
        for (String saxString : cluster) {
            saxStringSize = saxString.length();
            break;
        }

        // iterate through all the SAX strings of the cluster, calculating the average
        // char value for each position of the strings at a time.
        for (int i = 0; i < saxStringSize; i++) {
            char[] chars = new char[cluster.size()];  // chars carry all the i-th characters being read, that will be averaged
            for (int j = 0; j < clusterList.size(); j++) {
                chars[j] = clusterList.get(j).charAt(i);
            }

            centroid += CharUtils.calculateAverageChar(chars);
        }

        return centroid;
    }

    /* Calculate the centroid "position" by the datapoints of the cluster i
    The centroid will have the length of saxStringSize and composed by characters from 'a' to lastAlphabetLetter
     */
    private String[] getCentroids(ArrayList<LinkedHashSet<String>> clusters) {
        String[] centroids = new String[this.k];

        for (int i = 0; i < this.k; i++) {
            centroids[i] = getCentroid(clusters.get(i));
        }

        return centroids;
    }

    /*
    receive a list of clusters, the saxString to be added and the clusterID that
    the saxString need to be added. It updates the auxiliary structure 'clusters' 
    and the structure hmSaxCluster, that contains all the saxStrings and the
    cluster they belong to.
     */
    private void updateCluster(ArrayList<LinkedHashSet<String>> clusters, String saxString, int newClusterID) {
        // check if the sax string was already put in another cluster. If so, remove it
        if (this.hmSaxCluster.containsKey(saxString)) {
            int oldClusterID = this.hmSaxCluster.get(saxString);
            if (oldClusterID != newClusterID) {
                LinkedHashSet<String> oldCluster = clusters.get(oldClusterID);
                oldCluster.remove(saxString);
            }
        }

        // add saxString to the new cluster
        LinkedHashSet<String> cluster = clusters.get(newClusterID);  // load the cluster 'newClusterID'
        cluster.add(saxString);  // add the sax string in position i
        this.hmSaxCluster.put(saxString, newClusterID); //update the cluster ID of the sax string in the map
    }

    /*
    return an array of the same size of items which in each position is stored 
    the cluster of that item
     */
    public HashMap<String, Integer> getClusters() throws Exception {
        int listSize = saxStrings.size();
        int totalErrorOld = 0;
        int totalErrorNew = 0;  // assigned any value just to enter in the foor loop
        String[] centroids;

        // clusters is an auxiliary structure that allows indexed access
        // instantiate the empty clusters (sets) inside the list of clusters
        ArrayList<LinkedHashSet<String>> clusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            clusters.add(new LinkedHashSet<>());
        }

        centroids = getRandomCentroids();

        do {
            totalErrorOld = totalErrorNew;
            totalErrorNew = 0;

            // iterate through all sax strings comparing the distance to the centroids
            for (int i = 0; i < listSize; i++) {
                Integer minDistance = Integer.MAX_VALUE;
                String saxString = this.saxStrings.get(i);

                // iterate through all centroids
                int newDist;
                int newClusterID = 0;
                for (int clusterID = 0; clusterID < k; clusterID++) {
                    newDist = CharUtils.calculateDistance(saxString.toCharArray(), centroids[clusterID].toCharArray());
                    //newDist = distanceCalculator.apply(saxString, centroids[clusterID]);

                    // the sax string will be added to the cluster that has the mininum distance
                    // from its centroid to the sax String
                    if (newDist < minDistance) {
                        minDistance = newDist;
                        newClusterID = clusterID;
                    }
                }

                totalErrorNew += minDistance; // update the total error
                updateCluster(clusters, saxString, newClusterID);
            }

            centroids = getCentroids(clusters); // calculate the centroids for the next iteration
        } while (totalErrorOld != totalErrorNew);

        return hmSaxCluster;
    }

}
