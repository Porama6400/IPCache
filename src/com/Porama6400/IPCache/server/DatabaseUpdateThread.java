package com.Porama6400.IPCache.server;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DatabaseUpdateThread extends Thread {
    private final IPCacheServerCore core;

    public ConcurrentLinkedQueue<Runnable> statements = new ConcurrentLinkedQueue<Runnable>();
    private Runnable cache = null;
    private boolean running = true;

    public DatabaseUpdateThread(IPCacheServerCore ipCacheServerCore) {
        this.core = ipCacheServerCore;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                core.getLogger().info("DatabaseUpdateThread got interrupted!");
            }
            if ((cache = statements.poll()) != null) {
                cache.run();
            }
        }
    }
}
