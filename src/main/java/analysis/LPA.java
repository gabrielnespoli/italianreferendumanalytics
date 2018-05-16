/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analysis;

import static analysis.GraphAnalysis.RESOURCES_LOCATION;
import index.IndexSearcher;
import io.ReadFile;
import io.TxtUtils;
import static io.TxtUtils.txtToList;
import it.stilo.g.structures.LongIntDict;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.GraphReader;
import it.stilo.g.util.RandomTestNet;
import it.stilo.g.util.WeightedRandomGenerator;
import it.stilo.g.util.ZacharyNetwork;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.lucene.document.Document;
import twitter4j.JSONException;

public class LPA {

    public static boolean contains(final int[] array, final int key) {
        return ArrayUtils.contains(array, key);
    }

    public static void shuffleLists(List<Integer> l1, List<Integer> l2) {
        // shuffles randomly two lists in the same order
        long seed = System.nanoTime();
        Collections.shuffle(l1, new Random(seed));
        Collections.shuffle(l2, new Random(seed));
    }

    public static void shuffleArray(int[] array) {
        // shuffles randomly one array (from stackoverflow)
        int index;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            if (index != i) {
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
    }

    public static int getRandom(int[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    public static void promptEnterKey() {
        System.out.println("Press \"ENTER\" to continue...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public static int getPopularElement(int[] a) {
        // shuffle the labels so if there are 2 most popular nodes
        // the winner is selected randomly
        shuffleArray(a);
        // from stackoverflow
        int count = 0, tempCount;
        // add some randomness
        int popular = getRandom(a);
        int temp = getRandom(a);
        for (int i = 0; i < (a.length - 1); i++) {
            temp = a[i];
            tempCount = 0;
            for (int j = 1; j < a.length; j++) {
                if (temp == a[j]) {
                    tempCount++;
                }
            }
            if (tempCount > count) {
                popular = temp;
                count = tempCount;
            }
        }
        return popular;
    }

    public static void lpaAlgorithm(WeightedDirectedGraph g, int[] seedsYes, int[] seedsNo, String outputSufix, int killAt) throws IOException {

        // the rest of the nodes will be labeled with unique labels
        ArrayList<Integer> tmp = new ArrayList<Integer>();
        //int[] seedsUnknown = new int[g.size - seedsYes.length- seedsNo.length - 1];
        for (int i = 0; i < g.size; i++) {
            if ((contains(seedsYes, i) == false) && (contains(seedsNo, i) == false)) {
                tmp.add(i);
            }
        }
        // Create list of nodes that dont have yes or no label at the begining
        int[] seedsUnknown = tmp.stream().mapToInt(i -> i).toArray();

        // Put the labels, the position i contains the label of the element i of the graph
        int[] labels = new int[g.size];

        //-1 means yes, -2 means no, and 0 to n are reserved for the initialization of the unknown labels
        for (int i = 0; i < seedsYes.length; i++) {
            labels[seedsYes[i]] = -1;
        }
        for (int i = 0; i < seedsNo.length; i++) {
            labels[seedsNo[i]] = -2;
        }
        for (int i = 0; i < seedsUnknown.length; i++) {
            labels[seedsUnknown[i]] = i;
        }

        // create list of nodes
        int[] nodes = new int[labels.length];
        for (int i = 0; i < g.size; i++) {
            nodes[i] = i; // this has to be replaced by the real ID! but i use this because i dont see how to obtain an Array of IDs from g
        }

        // create a dict for the labels
        Map<Integer, Integer> nodesLabels = new HashMap<>();
        for (int i = 0; i < nodes.length; i++) {
            //System.out.println(nodes[i] + " " + labels[i]);
            int nodesTmp = nodes[i];
            int labelsTmp = labels[i];
            nodesLabels.put(nodesTmp, labelsTmp);
        }

        // go to each node and find the new labels
        // if this counter reaches the number of nodes, then all nodes
        // dont change label at iteration k and we can stop iterating
        int counterConvergence = 0;

        // put a limit of iterations and keep track of the iterations for convergence
        int counter = 0;

        // Store here the temporal labels
        ArrayList<Integer> yesCounterOverTime = new ArrayList<Integer>();
        ArrayList<Integer> noCounterOverTime = new ArrayList<Integer>();
        ArrayList<Integer> unknownCounterOverTime = new ArrayList<Integer>();
        yesCounterOverTime.add(seedsYes.length);
        noCounterOverTime.add(seedsNo.length);
        unknownCounterOverTime.add(seedsUnknown.length);

        // change this when convergence is done or killAt < counter
        boolean stop = false;
        int newLabelNode;
        while (stop == false) {
            counter++;
            counterConvergence = 0;

            // shuffle first
            shuffleArray(nodes);

            // check iteration counter to save results
            if (counter % 5 == 0) {
                // j is an exact multiple of 4
                System.out.println("Saving counters");
                TxtUtils.iterableToTxt(RESOURCES_LOCATION + "yes_labels_temporal_counter" + outputSufix + ".txt", yesCounterOverTime);
                TxtUtils.iterableToTxt(RESOURCES_LOCATION + "no_labels_temporal_counter" + outputSufix + ".txt", noCounterOverTime);
                TxtUtils.iterableToTxt(RESOURCES_LOCATION + "unknown_labels_temporal_counter" + outputSufix + ".txt", unknownCounterOverTime);
            }

            // print counters of this iteration
            System.out.println("Iteration " + counter + " counting...");
            int lenthis = yesCounterOverTime.size() - 1;
            System.out.println("Yes labels: " + yesCounterOverTime.get(lenthis));
            lenthis = noCounterOverTime.size() - 1;
            System.out.println("No labels: " + noCounterOverTime.get(lenthis));
            lenthis = unknownCounterOverTime.size() - 1;
            System.out.println("Unknown labels: " + unknownCounterOverTime.get(lenthis));
            System.out.println("");

            // start the counters for the new iteration
            yesCounterOverTime.add(0);
            noCounterOverTime.add(0);
            unknownCounterOverTime.add(0);

            // go to each node
            for (int k = 0; k < nodes.length; k++) {
                //node of this iteration
                int node = nodes[k];
                // get the neighbours of node
                int[] neighbours = g.in[node];
                newLabelNode = nodesLabels.get(node);
                //System.out.println("iter: " + counter + " node: " + node + " label: " + nodesLabels.get(node));

                // if there is at least 1 neighbour get the most popular label of neighbours
                // if not, keep current label
                if (neighbours != null) {
                    // get the array of labels
                    int[] neighbourLabels = new int[neighbours.length];
                    for (int j = 0; j < neighbours.length; j++) {
                        //System.out.println(j + " "+ neighbours[j] + " " + neighbours.length);
                        int nj = neighbours[j];
                        int tmpLabel = nodesLabels.get(nj);
                        neighbourLabels[j] = tmpLabel;
                        //System.out.println(neighbourLabels[j]);
                    }
                    // Make a filter to get only labels from yes and no
                    ArrayList<Integer> neighbourLabelsYesNo = new ArrayList<Integer>();
                    for (int nl = 0; nl < neighbourLabels.length; nl++) {
                        int neighbourLabelsnl = neighbourLabels[nl];
                        if (neighbourLabelsnl == -1 || neighbourLabelsnl == -2) {
                            neighbourLabelsYesNo.add(neighbourLabelsnl);
                        }
                    }
                    if (neighbourLabelsYesNo.size() > 0) {
                        int[] neighbourLabelsYesNoArray = neighbourLabelsYesNo.stream().mapToInt(i -> i).toArray();
                        newLabelNode = getPopularElement(neighbourLabelsYesNoArray);
                    } else {
                        newLabelNode = nodesLabels.get(node);
                    }

                    //System.out.println("has nn - node: " + node + " len: " + neighbourLabels.length + " first n: "+ neighbours[0] +
                    //        " first n label: " + neighbourLabels[0] +" most popular " + newLabelNode);
                    //System.out.println(java.util.Arrays.toString(g.in[node]));
                    //promptEnterKey();
                }
                // check if the old label is the same as the new one
                if (nodesLabels.get(node).equals(newLabelNode)) {
                    counterConvergence++;
                }

                // Update the label
                nodesLabels.replace(node, newLabelNode);

                // modify temporal counters
                if (newLabelNode == -1) {
                    int len = yesCounterOverTime.size() - 1;
                    int counterLast = yesCounterOverTime.get(len);
                    yesCounterOverTime.remove(len);
                    yesCounterOverTime.add(counterLast + 1);
                } else if (newLabelNode == -2) {
                    int len = noCounterOverTime.size() - 1;
                    int counterLast = noCounterOverTime.get(len);
                    noCounterOverTime.remove(len);
                    noCounterOverTime.add(counterLast + 1);
                } else {
                    int len = unknownCounterOverTime.size() - 1;
                    int counterLast = unknownCounterOverTime.get(len);
                    unknownCounterOverTime.remove(len);
                    unknownCounterOverTime.add(counterLast + 1);
                }
            }
            // check convergence
            if (counterConvergence == nodes.length) {
                System.out.println("Converged at " + counter);
                stop = true;
            }
            if (counter >= killAt) {
                System.out.println("Did not converge yet after steps: " + counter);
                stop = true;
            }

        }

        int counterYes = 0;
        int counterNo = 0;
        int counterUnknown = 0;
        System.out.println("Counting...");
        for (int i = 0; i < nodes.length; i++) {
            //System.out.println("");
            //System.out.println(nodes[i] + ": " + nodesLabels.get(nodes[i]));
            //System.out.println(java.util.Arrays.toString(g.in[nodes[i]]));
            if (nodesLabels.get(nodes[i]).equals(-1)) {
                counterYes++;
            } else if (nodesLabels.get(nodes[i]).equals(-2)) {
                counterNo++;
            } else {
                counterUnknown++;
            }

        }

        // print final results
        System.out.println("Yes labels: " + counterYes);
        System.out.println("No labels: " + counterNo);
        System.out.println("Unknown labels: " + counterUnknown);

        // save to files the temporal counters
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "yes_labels_temporal_counter" + outputSufix + ".txt", yesCounterOverTime);
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "no_labels_temporal_counter" + outputSufix + ".txt", noCounterOverTime);
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "unknown_labels_temporal_counter" + outputSufix + ".txt", unknownCounterOverTime);
        //List<Integer> labelsIterable = Arrays.asList(labels);
        //TxtUtils.iterableToTxt(RESOURCES_LOCATION + "labels_final_step.txt", labelsIterable);
    }

    public static void convertUsernameToId(String inputSufix, String outputSufix) throws FileNotFoundException, IOException, org.apache.lucene.queryparser.classic.ParseException {
        // creates M files with twitter IDs instead of usernames

        String[] prefixYesNo = {"yes", "no"};
        ArrayList<String> seedYes = new ArrayList<String>();
        ArrayList<String> seedNo = new ArrayList<String>();

        // Load the IDs of the users
        for (String supportType : prefixYesNo) {
            List<String> usersList = txtToList(RESOURCES_LOCATION + supportType + inputSufix); // retrieve the users names
            String[] nodes = new String[usersList.size()];
            int i = 0;
            // from the users names to their Twitter ID, then to their respective position in the graph (int)
            // name -> twitterID -> position in the graph
            for (String username : usersList) {
                Document[] docs = IndexSearcher.searchByField("all_tweets_index/", "user", username, 1);

                if (docs != null) {
                    String id = docs[0].get("id"); //read just the first resulting doc
                    nodes[i] = id;  // retrieve the twitter ID (long) and covert to int (the position in the graph)
                    if ("yes".equals(supportType)) {
                        seedYes.add(nodes[i]);
                    } else if ("no".equals(supportType)) {
                        seedNo.add(nodes[i]);
                    }
                }
                i++;
            }

        }
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "no" + outputSufix, seedNo);
        TxtUtils.iterableToTxt(RESOURCES_LOCATION + "yes" + outputSufix, seedYes);

    }

    private static int[] loadSeed(LongIntDict mapLong2Int, String prefix, String sufix) throws IOException {
        // read no and yes M IDs
        //List<String> seedNo = TxtUtils.txtToList(RESOURCES_LOCATION + "no_M_IDs.txt");
        List<String> seed = TxtUtils.txtToList(RESOURCES_LOCATION + prefix + sufix);

        // save here the ids after mapping
        int[] seeds = new int[seed.size()];

        // Map them into the graph IDs
        for (int i = 0; i < seed.size(); i++) {
            seeds[i] = mapLong2Int.get(Long.parseLong(seed.get(i)));
        }
        for (int i = 0; i < seed.size(); i++) {
            seeds[i] = mapLong2Int.get(Long.parseLong(seed.get(i)));
        }
        return seeds;
    }

    public static void runLPA() throws FileNotFoundException, IOException, org.apache.lucene.queryparser.classic.ParseException, InterruptedException {

        // Create a fake graph for testing
        /*WeightedDirectedGraph g = new WeightedDirectedGraph(ZacharyNetwork.VERTEX);
        ZacharyNetwork.generate(g, 2);

        // Indices of nodes from group 1 (seeds)
        int[] seedsYes = new int[]{1, 2, 3, 4, 5, 6, 7};
        // Indices of nodes of group 2
        int[] seedsNo = new int[]{14, 15, 16, 17};

        lpaAlgorithm(g, seedsYes, seedsNo, "test.txt");
         */
 
        
        
        // put the labels in the right format
        convertUsernameToId("_M.txt", "_M_IDs.txt");
        convertUsernameToId("_top500_hub_auth.txt", "_top500_hub_auth_IDs.txt");
        // use this line manually before running the next convert
        // cat yes_top_k_players.txt | cut -f1 -d"," | sed "s/(//g" > yes_top_k_players_clean.txt
        convertUsernameToId("_top_k_players_clean.txt", "_top_k_players_clean_IDs.txt");

        // load graph and mapper
        int graphSize = 16815933;
        WeightedDirectedGraph g = new WeightedDirectedGraph(graphSize + 1);
        String graphFilename = "Official_SBN-ITA-2016-Net.gz";
        LongIntDict mapLong2Int = new LongIntDict();
        GraphReader.readGraphLong2IntRemap(g, RESOURCES_LOCATION + graphFilename, mapLong2Int, false);
        
        // run for M
        int[] seedsYes = loadSeed(mapLong2Int, "yes", "_M_IDs.txt");
        int[] seedsNo = loadSeed(mapLong2Int, "no", "_M_IDs.txt");
        lpaAlgorithm(g, seedsYes, seedsNo, "_M_", 20);
        
        // run for M'
        seedsYes = loadSeed(mapLong2Int, "yes", "_top500_hub_auth_IDs.txt");
        seedsNo = loadSeed(mapLong2Int, "no", "_top500_hub_auth_IDs.txt");
        lpaAlgorithm(g, seedsYes, seedsNo, "_Mprime_", 20);
        
        //run for top k players
        seedsYes = loadSeed(mapLong2Int, "yes", "_top_k_players_clean_IDs.txt");
        seedsNo = loadSeed(mapLong2Int, "no", "_top_k_players_clean_IDs.txt");
        lpaAlgorithm(g, seedsYes, seedsNo, "_players_", 20);
    }

}
