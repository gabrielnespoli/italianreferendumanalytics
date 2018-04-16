package preprocess;

import twitter4j.JSONException;
import twitter4j.JSONObject;

public class Parser {

    public static String dataBeginMarkup = "[CDATA[";
    public static String dataEndMarkup = "]]></p></o>";

    private static String getData(String xml) {
        int dataBegin = xml.indexOf(dataBeginMarkup) + dataBeginMarkup.length();
        int dataEnd = xml.indexOf(dataEndMarkup);

        return xml.substring(dataBegin, dataEnd);
    }

    public static JSONObject getJSON(String xml) throws JSONException {
        JSONObject json = new JSONObject(getData(xml));
        return json;
    }
}
