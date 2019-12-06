package com.Porama6400.IPCache.server.net.nettyserver;

import com.Porama6400.IPCache.server.IPCacheServerCore;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;

public class APIServer {
    private final IPCacheServerCore core;

    private final EventLoopGroup master = new NioEventLoopGroup();
    private final EventLoopGroup slave = new NioEventLoopGroup();
    ChannelFuture channel;

    public APIServer(IPCacheServerCore core) throws IOException, InterruptedException {
        this.core = core;
        core.getLogger().info("HTTP Server starting...");

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(master, slave)
                .channel(NioServerSocketChannel.class)
                .childHandler(new APIServerChannelInitializer(core));

        channel = bootstrap.bind(core.getConfig().apiPort).sync();

        core.getLogger().info("HTTP Server started on http://127.0.0.1:" + core.getConfig().apiPort + "/" + core.getConfig().apiLocation);
    }

    public void close() {
        channel.channel().close();
        master.shutdownGracefully();
        slave.shutdownGracefully();
    }

}

