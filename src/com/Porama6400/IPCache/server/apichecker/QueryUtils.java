package com.Porama6400.IPCache.server.apichecker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryUtils {

    public static Pattern regexPSIOJsonExtractor = Pattern.compile("\"(.*)\": \"(.*)\"");

    public static String read(URL url) throws IOException {
        HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 8.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");

        StringBuilder out = new StringBuilder();
        InputStreamReader input = new InputStreamReader(httpcon.getInputStream());

        while (true) {
            char[] buffer = new char[1024];
            int result = input.read(buffer, 0, buffer.length);
            if (result == -1) break;
            out.append(buffer, 0, result);
        }
        return out.toString();
    }

    public static String read(String stringURL) throws IOException {
        return read(new URL(stringURL));
    }

    public static HashMap<String, String> mapPSIOJsonResponse(String in) {
        HashMap<String, String> out = new HashMap<>();
        Matcher matcher = regexPSIOJsonExtractor.matcher(in);

        while (matcher.find()) {
            out.put(matcher.group(1), matcher.group(2));
        }

        return out;
    }
}
