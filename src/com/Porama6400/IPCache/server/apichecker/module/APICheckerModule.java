package com.Porama6400.IPCache.server.apichecker.module;

import com.Porama6400.IPCache.server.apichecker.APICheckerInterface;
import com.Porama6400.IPCache.server.apichecker.QueryUtils;

import java.io.IOException;

public interface APICheckerModule extends APICheckerInterface {


    default String readRaw(String IP) throws IOException {
        return QueryUtils.read(getURL(IP));
    }

    String getURL(String IP);


    void setAvailable(boolean b);

    String getStatusString();
}
