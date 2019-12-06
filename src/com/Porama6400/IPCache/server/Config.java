package com.Porama6400.IPCache.server;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public String databaseURL = "jdbc:mysql://localhost:3306/[database]";
    public String databaseUser = "[user]";
    public String databasePass = "[pass]";
    public String databaseTable = "[table]";
    public int databasePoolSize = 4;
    public boolean enableDatabaseUpdate = true;

    public int apiPort = 5888;
    public String apiLocation = "815a3649u2c66786264u8113"; // http://localhost:port/apiLocation

    public int cleanupDelay = 60; // in minutes
    public int databaseCacheTime = 14; //in days
    public List<String> apiKeys = new ArrayList<>();
}
