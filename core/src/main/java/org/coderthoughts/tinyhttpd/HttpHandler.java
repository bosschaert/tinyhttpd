package org.coderthoughts.tinyhttpd;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * The HttpHandler handles the HTTP requests. It is called by Netty when a HTTP request
 * needs to be processed.
 */
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final GetHandler getHandler;
    private final PostHandler postHandler;

    public HttpHandler(String webRoot) {
        getHandler = new GetHandler(webRoot);
        postHandler = new PostHandler(webRoot);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.getDecoderResult().isSuccess()) {
            BaseHandler.sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }


        if (request.getMethod() == HttpMethod.GET) {
            getHandler.handleGetRequest(ctx, request);
            return;
        } else if (request.getMethod() == HttpMethod.POST) {
            postHandler.handlePostRequest(ctx, request);
            return;
        }
        BaseHandler.sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
    }
}
