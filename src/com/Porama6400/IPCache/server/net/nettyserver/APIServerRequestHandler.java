package com.Porama6400.IPCache.server.net.nettyserver;

import com.Porama6400.IPCache.server.IPCacheServerCore;
import com.Porama6400.IPCache.server.apichecker.APIResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APIServerRequestHandler extends ChannelInboundHandlerAdapter {
    private final IPCacheServerCore core;
    private Pattern pattern = Pattern.compile("\\/(.*)\\?ip=(.*)");

    public APIServerRequestHandler(IPCacheServerCore core) {
        this.core = core;
    }

    public boolean check(String ip) {
        try {
            APIResult result = core.get(ip);
            if (result == null) return false; //In case of failed connection
            return core.isVPN(result);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            if (!core.isReady()){
                ctx.close();
                return;
            }
            final FullHttpRequest request = (FullHttpRequest) msg;

            Matcher matcher = pattern.matcher(request.uri());
            if (matcher.find()) {
                if (matcher.group(1).equals(core.getConfig().apiLocation)) {
                    String ip = matcher.group(2);
                    boolean isvpn = false;
                    char resultSource = '?';

                    APIResult result = core.get(ip);
                    if (result != null) {
                        isvpn = core.isVPN(result);
                        switch (result.getSource()) {
                            case RAM_CACHE:
                                resultSource = 'R';
                                break;
                            case EXTERNAL_API:
                                resultSource = 'X';
                                break;
                            case DATABASE_CACHE:
                                resultSource = 'D';
                                break;
                        }
                    }


                    String message = (ip + "|vpn=" + (isvpn ? "true" : "false"));
                    core.getLogger().info("[" + resultSource + "]" + (isvpn ? "[V]" : "[ ]") + " " + ip);
                    ByteBuf messageBuf = Unpooled.copiedBuffer(message.getBytes());
                    FullHttpResponse response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.OK,
                            messageBuf
                    );
                    response.headers().set("Content-Type", "text/plain");
                    response.headers().set("Content-Length", message.length());
                    response.headers().set("Server", "Apache/2.4.18 (Ubuntu)");
                    ctx.write(response);
                    ctx.flush();
                    ctx.close();

                    core.cleanUp(false); //Clean up if needed
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
