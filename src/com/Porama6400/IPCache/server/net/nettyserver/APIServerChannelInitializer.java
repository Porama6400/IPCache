package com.Porama6400.IPCache.server.net.nettyserver;

import com.Porama6400.IPCache.server.IPCacheServerCore;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class APIServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final IPCacheServerCore core;

    public APIServerChannelInitializer(IPCacheServerCore core) {
        this.core = core;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipe = ch.pipeline();
        pipe.addLast(new HttpServerCodec());
        pipe.addLast(new HttpObjectAggregator(65535));
        pipe.addLast(new APIServerRequestHandler(core));
    }
}
