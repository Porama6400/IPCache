package com.Porama6400.IPCache.server.apichecker;

import com.Porama6400.IPCache.server.apichecker.module.APICheckerModule;

import java.io.IOException;
import java.util.ArrayList;

public class APICheckerLoadBalancer extends ArrayList<APICheckerModule> implements APICheckerInterface {
    @Override
    public boolean checkAvailability() {
        for (APICheckerModule api : this) {
            api.checkAvailability();
        }

        return isAvailable();
    }

    @Override
    public boolean isAvailable() {
        for (APICheckerModule api : this) {
            if (api.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public APIResult check(String IP) throws IOException {
        APIResult result;

        //RETRIES 3 TIMES BEFORE GIVE UP
        for (int i = 0; i < 3; i++) {

            for (APICheckerModule api : this) {
                if (api.isAvailable()) {
                    result = api.check(IP);
                    if (result.getResult() != APIResult.QueryResult.OK) {
                        checkAvailability();
                        break;
                    }
                    return result;
                }
            }

        }

        //RETURN "ERROR" RESULT
        result = new APIResult();
        result.setResult(APIResult.QueryResult.LOAD_BALANCER_ERROR);
        return result;
    }
}
