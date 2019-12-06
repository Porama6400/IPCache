package com.Porama6400.IPCache.server.apichecker.module;

import com.Porama6400.IPCache.server.apichecker.APIResult;
import com.Porama6400.IPCache.server.apichecker.APIResult.QueryResult;
import com.Porama6400.IPCache.server.apichecker.QueryUtils;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class APICheckerProxyCheckIO implements APICheckerModule {
    private String APIKey;
    private boolean available = false;

    private Pattern statusExtractor = Pattern.compile("(?:\"status\": \")(.*)(?:\")");

    public APICheckerProxyCheckIO(String APIKey) {
        this.APIKey = APIKey;
    }

    public static APIResult interpretRawData(APIResult result, String raw) {
        Map<String, String> dataMap = QueryUtils.mapPSIOJsonResponse(raw);
        {
            String resultString = dataMap.get("status");
            if (resultString.equalsIgnoreCase("denied")) {
                result.setResult(QueryResult.DENIED);
                return result;
            } else if (resultString.equalsIgnoreCase("error")) {
                result.setResult(QueryResult.ERROR);
                return result;
            } else if (resultString.equalsIgnoreCase("ok")) result.setResult(QueryResult.OK);
        }

        if (dataMap.containsKey("asn")) {
            result.setASN(dataMap.get("asn"));
        }

        if (dataMap.containsKey("provider")) {
            result.setISP(dataMap.get("provider"));
        }

        if (dataMap.containsKey("country")) {
            result.setCountry(dataMap.get("country"));
        }

        if (dataMap.get("proxy").equals("yes")) {
            result.setVPN(true);
            if (dataMap.containsKey("type")) {
                result.setClientType(dataMap.get("type"));
            }
        } else {
            result.setVPN(false);
            result.setClientType(null);
        }

        return result;
    }

    @Override
    public String getURL(String IP) {
        return "http://proxycheck.io/v2/" + IP + "?" + (APIKey == null ? "" : ("key=" + APIKey + "&")) + "vpn=1&asn=1&time=1";
    }

    @Override
    public boolean checkAvailability() {
        try {
            APIResult result = check("216.58.196.46");
            this.available = (result.getResult() == QueryResult.OK);
            return this.available;

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void setAvailable(boolean b) {
        this.available = b;
    }

    @Override
    public String getStatusString() {
        return (available ? "available" : "offline");
    }

    @Override
    public APIResult check(String IP) throws IOException {
        APIResult result = new APIResult();
        result.setIP(IP);

        String data = readRaw(IP);
        result = interpretRawData(result,data);
        return result;
    }

    public String getAPIKey() {
        return APIKey;
    }

    @Override
    public String toString() {
        return getAPIKey();
    }
}
