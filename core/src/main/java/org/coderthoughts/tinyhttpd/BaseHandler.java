package org.coderthoughts.tinyhttpd;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

abstract class BaseHandler {
    private final String webRoot;

    public BaseHandler(String webRoot) {
        this.webRoot = webRoot;
    }

    String getPathFromUri(ChannelHandlerContext ctx, String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException e1) {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                return null;
            }
        }

        if (!uri.startsWith("/")) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return null;
        }

        if (webRoot == null) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return null;
        }

        uri = uri.replace('/', File.separatorChar);
        String path = webRoot + File.separator + uri;

        if (uri.trim().endsWith("/")) {
            // Serve the index.html file if it exists in the directory
            if (new File(path + "index.html").isFile()) {
                sendRedirect(ctx, uri + "index.html");
                return null;
            }
        }

        return path;
    }

    static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    static void sendRedirect(ChannelHandlerContext ctx, String newLocation) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaders.Names.LOCATION, newLocation);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
