package preprocess;

import io.CSVUtils;
import utils.TwitterUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import twitter4j.TwitterException;

public abstract class PoliticiansLoader {

    private static List<String[]> fromMatrixNameToMatrixTwitterID(List<String[]> politiciansMatrix) {
        politiciansMatrix.forEach((politiciansLine) -> {
            //jump the header
            if (politiciansLine != politiciansMatrix.get(0)) {
                String politicianYes = politiciansLine[0];
                String politicianNo = "";

                if (politiciansLine.length > 1) {
                    politicianNo = politiciansLine[1];
                }

                try {
                    if (!politicianYes.startsWith("@")) {
                        politicianYes = TwitterUtils.fromNameToTwitterScreenName(politicianYes);
                        politiciansLine[0] = politicianYes;
                    }
                    if (!politicianNo.startsWith("@") && !"".equals(politicianNo)) {
                        politicianNo = TwitterUtils.fromNameToTwitterScreenName(politicianNo);
                        politiciansLine[1] = politicianNo;
                    }
                } catch (InterruptedException ie) {
                    System.out.println(ie.getMessage());
                }
            }
        });

        return politiciansMatrix;
    }

    public static void main(String[] args) throws TwitterException, FileNotFoundException, IOException {
        String csvFile = "src/main/resources/politicians.csv";
        List<String[]> politicians = CSVUtils.readCSV(csvFile, ";");
        politicians = fromMatrixNameToMatrixTwitterID(politicians);
        CSVUtils.writeCSV(politicians, "src/main/resources/politicians_loaded.csv");
    }
}