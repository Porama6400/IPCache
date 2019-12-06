package com.Porama6400.IPCache.server;

import com.Porama6400.IPCache.server.apichecker.APICheckerLoadBalancer;
import com.Porama6400.IPCache.server.apichecker.APIResult;
import com.Porama6400.IPCache.server.apichecker.QueryUtils;
import com.Porama6400.IPCache.server.apichecker.module.APICheckerModule;
import com.Porama6400.IPCache.server.apichecker.module.APICheckerProxyCheckIO;
import com.Porama6400.IPCache.server.net.MySQLAdapter;
import com.Porama6400.IPCache.server.net.nettyserver.APIServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class IPCacheServerCore {
    private final Config config;
    public DatabaseUpdateThread databaseUpdateThread = new DatabaseUpdateThread(this);
    private APICheckerLoadBalancer checkPool = new APICheckerLoadBalancer();
    private APIServer apiServer;
    private MySQLAdapter sqlAdapter;
    private Logger logger;
    private FileHandler logFileHandler;
    private boolean strictMode = false;
    private boolean ready = false;

    //Console
    private BufferedReader consoleReader;
    private long lastCleanupExecution = System.currentTimeMillis();
    private long cleanupExecutionDelay;
    private Map<String, APIResult> resultCache = new HashMap<>();

    public IPCacheServerCore(Config config) throws IOException, SQLException, InterruptedException {
        this.config = config;

        for (String apiKey : config.apiKeys) {
            checkPool.add(new APICheckerProxyCheckIO(apiKey));
        }

        consoleReader = new BufferedReader(new InputStreamReader(System.in));

        logger = Logger.getLogger(this.getClass().getName());
        ConsoleHandler consoleHandler = new ConsoleHandler();
        {   //DELETE OLD LOG FILE
            File log = new File("./log.txt");
            if (log.exists()) {
                log.delete();
            }
            log.createNewFile();
        }
        logFileHandler = new FileHandler("./log.txt");

        consoleHandler.setFormatter(new LogFormatter(false));
        logFileHandler.setFormatter(new LogFormatter(true));

        logger.setUseParentHandlers(false);
        logger.addHandler(consoleHandler);
        logger.addHandler(logFileHandler);

        logger.info("Initializing API Server...");
        apiServer = new APIServer(this);

        logger.info("Initializing MySQL Client...");

        if (this.config.enableDatabaseUpdate) databaseUpdateThread.start();
        sqlAdapter = new MySQLAdapter(this, this.config);
        sqlAdapter.init();
        logger.info("Initializing MySQL Client completed!");
        cleanupExecutionDelay = this.config.cleanupDelay * 60000; //Convert minutes to millisec
        ready = true;
        logger.info("Server ready!");
    }

    public Map<String, APIResult> getResultCache() {
        return resultCache;
    }

    public DatabaseUpdateThread getDatabaseUpdateThread() {
        return databaseUpdateThread;
    }


    public boolean run() throws IOException, SQLException {
        String consoleInputStringBuffer;
        if ((consoleInputStringBuffer = consoleReader.readLine()) != null) {
            String[] args = consoleInputStringBuffer.split(" ");
            String command = args[0];
            return consoleHandler(command, args);
        }

        return true;
    }

    private boolean consoleHandler(String input, String[] args) throws IOException, SQLException {
        if (input.equalsIgnoreCase("stop")) {
            logger.info("Shutting down API server...");
            apiServer.close();
            logger.info("Closing MySQL connection...");
            sqlAdapter.close();
            logger.info("Stopping MySQL update thread...");
            databaseUpdateThread.setRunning(false);
            logFileHandler.close();
            return false; //EXIT THE LOOP

        } else if (input.equalsIgnoreCase("ping")) {
            try {
                logger.info("Current database ping: " + pingDatabase());
            } catch (SQLException e) {
                logger.info("Error while pinging the server!!!");
                e.printStackTrace();
            }

        } else if (input.equalsIgnoreCase("httpget")) {
            if (args.length < 2) {
                logger.info("No URL specified!");
                return true;
            } else {
                String stringURL = args[1];

                logger.info("======================================");
                logger.info(QueryUtils.read(stringURL));
                logger.info("======================================");
            }
        } else if (input.equalsIgnoreCase("get")) {
            APIResult result = get(args[1]);
            logger.info("======================================");
            logger.info("IP: " + result.getIP());
            logger.info("Type: " + result.getClientType());
            logger.info("Source: " + result.getSource().toString());
            logger.info("");
            logger.info("ASN: " + result.getASN());
            logger.info("ISP: " + result.getISP());
            logger.info("Country: " + result.getCountry());
            logger.info("======================================");
        } else if (input.equalsIgnoreCase("flush")) {
            if (resultCache.containsKey(args[1])) {
                resultCache.remove(args[1]);
                logger.info("IP " + args[1] + " removed from RAM cache!");
            } else {
                logger.info("IP " + args[1] + " not found on RAM cache!");
            }
        } else if (input.equalsIgnoreCase("whitelist")) {
            resultCache.remove(args[1]);
            if (sqlAdapter.allowUser(args[1])) {
                logger.info("IP " + args[1] + " successfully temporary whitelisted!");
            } else {
                logger.info("IP " + args[1] + " does not existed on the Database!");
            }
        } else if (input.equalsIgnoreCase("cleanup")) {
            cleanUp(true);
        } else if (input.equalsIgnoreCase("spread")) {
            int updateCount = sqlAdapter.spread();
            logger.info("Cache expiry date had been spread! ( " + updateCount + " row(s) updated) ");
        } else if (input.equalsIgnoreCase("strict")) {
            strictMode = !strictMode;
            logger.info("Strict mode is now " + (strictMode ? "enabled" : "disabled") + "!");
        } else if (input.equalsIgnoreCase("status")) {
            logger.info("==============================");
            logger.info("Last RAM cache cleanup is " + ((System.currentTimeMillis() - lastCleanupExecution) / 60000) + " minutes ago");
            logger.info("Next RAM cache cleanup in " + ((cleanupExecutionDelay - (System.currentTimeMillis() - lastCleanupExecution)) / 60000) + " minutes");
            logger.info("==============================");
            logger.info("IP cached in RAM: " + resultCache.size());
            logger.info("VPN cached in RAM: " + countRAMCacheVPN());
            logger.info("Database rows: " + sqlAdapter.countRow());
            logger.info("Database VPNs: " + sqlAdapter.countVPN());
            logger.info("==============================");
            logger.info("API key status:");
            for (APICheckerModule mod : checkPool) {
                logger.info(mod.toString() + " : " + mod.getStatusString());
            }
            logger.info("==============================");
        } else if (input.equalsIgnoreCase("triggerdebug")) {
            logger.info("Debug triggered! (hopefully)");
        } else {
            logger.info("Unknown command!");
        }

        return true;
    }

    public void cleanUp(boolean force) throws SQLException {
        if (!force && System.currentTimeMillis() < lastCleanupExecution + cleanupExecutionDelay) {
            return;
        }
        lastCleanupExecution = System.currentTimeMillis();
        logger.info("Cleaning up cache..." + resultCache.size() + " entries deleted");
        resultCache.clear();
        logger.info("Cleaning up database... " + sqlAdapter.clean() + " row(s) deleted");
        logger.info("Cleaning process done!");
    }

    public Config getConfig() {
        return config;
    }

    public APIServer getApiServer() {
        return apiServer;
    }

    public MySQLAdapter getSqlAdapter() {
        return sqlAdapter;
    }

    public APICheckerLoadBalancer getCheckPool() {
        return checkPool;
    }

    public int countRAMCacheVPN() {
        int count = 0;
        for (Map.Entry<String, APIResult> result : resultCache.entrySet())
            if (isVPN(result.getValue())) count++;
        return count;
    }

    /**
     * This is the main function to get VPN report result
     */
    public APIResult get(String IP) throws SQLException, IOException {

        // Read from RAM cache if available
        if (resultCache.containsKey(IP)) {
            final APIResult result = resultCache.get(IP);
            result.setSource(APIResult.ResultSource.RAM_CACHE);
            return result;
        }

        APIResult out = null;

        // If database failed then get directly from API
        if (sqlAdapter.con.isClosed()) {
            logger.severe("Database connection failed!");
            out = checkPool.check(IP);
            out.setSource(APIResult.ResultSource.EXTERNAL_API);
            resultCache.put(IP, out);
            return out;
        }

        // Query from our own VPN database
        String sqlCommand = "SELECT vpn, type, isp, asn, country FROM " + config.databaseTable + " WHERE ip = ?;";
        PreparedStatement statement = sqlAdapter.con.prepareStatement(sqlCommand);
        statement.setString(1, IP);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            out = new APIResult();
            out.setSource(APIResult.ResultSource.DATABASE_CACHE);
            out.setIP(IP);
            out.setVPN(result.getBoolean(1));
            out.setClientType(result.getString(2));
            out.setISP(result.getString(3));
            out.setASN(result.getString(4));
            out.setCountry(result.getString(5));
        } else {
            // If there aren't record in database, get from API and keep them in database
            out = checkPool.check(IP);
            out.setSource(APIResult.ResultSource.EXTERNAL_API);
            sqlAdapter.updateDatabase(IP, out);
        }

        // Put it in RAM caching
        resultCache.put(IP, out);

        return out;
    }

    public int pingDatabase() throws SQLException {
        Statement statement = sqlAdapter.con.createStatement();
        long timeStart = System.currentTimeMillis();
        statement.execute("/* ping */ SELECT 1"); //PING THE SERVER "/* ping */" is required
        timeStart = System.currentTimeMillis() - timeStart;
        return (int) timeStart;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public boolean isVPN(APIResult apiResult) {

        if (apiResult.getClientType() != null && !strictMode) {
            if (apiResult.getClientType().equals("Compromised Server")) return false;
        }

        return apiResult.isVPN();
    }

    public boolean isReady() {
        return ready;
    }
}
