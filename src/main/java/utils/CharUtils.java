package utils;

import java.util.HashMap;

/* 
Class that provide a method to calculate the average char in a sequence of chars. 
Ex: calculateAverageChar([a,c]) = b
Provides also a static method to calculate distance from 2 vector of chars
 */
public abstract class CharUtils {

    private static final HashMap<Character, Integer> hmCharInt;
    private static final HashMap<Integer, Character> hmIntChar;

    static {
        hmCharInt = new HashMap<>();
        hmIntChar = new HashMap<>();

        // iterate through all the chars (from a to z)
        for (int c = 97, value = 0; c < 123; c++, value++) {
            hmCharInt.put((char) c, value);
            hmIntChar.put(value, (char) c);
        }
    }

    public static char calculateAverageChar(char[] chars) {
        int average = 0;
        for (int i = 0; i < chars.length; i++) {
            average += hmCharInt.get(chars[i]);
        }
        average = (int) Math.round(average / chars.length);

        return hmIntChar.get(average);
    }

    /*
    Calculate the distance between two vector of chars. The total distance is a summation
    of the distance of each char in the vectors. A distance from two chars is their
    difference in ASCII value
    */
    public static int calculateDistance(char[] chars1, char[] chars2) throws Exception {
        if (chars1.length != chars2.length) {
            throw new Exception("Vector of chars with different size");
        }

        int totalDistance = 0;
        for (int i = 0; i < chars1.length; i++) {
            totalDistance += Math.abs(chars1[i] - chars2[i]);
        }

        return totalDistance;
    }

}
