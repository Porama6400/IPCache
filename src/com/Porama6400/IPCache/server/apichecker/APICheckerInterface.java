package com.Porama6400.IPCache.server.apichecker;

import java.io.IOException;

public interface APICheckerInterface {


    boolean checkAvailability();

    default boolean isAvailable() {
        return true;
    }

    APIResult check(String IP) throws IOException;
}
