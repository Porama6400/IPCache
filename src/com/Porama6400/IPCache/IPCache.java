package com.Porama6400.IPCache;

import com.Porama6400.IPCache.server.Config;
import com.Porama6400.IPCache.server.IPCacheServerCore;
import com.Porama6400.IPCache.server.apichecker.module.APICheckerProxyCheckIO;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class IPCache {
    public static void main(String[] args) throws SQLException, IOException, InterruptedException {


        Yaml yaml = new Yaml();
        FileReader freader = new FileReader(new File("./config.yml"));
        Config config = yaml.load(freader);
        freader.close();

        IPCacheServerCore ipcs = new IPCacheServerCore(config);
        ipcs.getCheckPool().checkAvailability();


        boolean run = true;
        while (run) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                run = ipcs.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
