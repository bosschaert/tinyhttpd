package org.coderthoughts.tinyhttpd;

import io.netty.buffer.ByteBuf;
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

/**
 * The base class for both the Get Handler and the Post Handler. This class contains shared
 * functionality used by both these subclasses.
 */
abstract class BaseHandler {
    private final String webRoot;

    /**
     * Constructor. The web-root location must be provided.
     * @param webRoot The location on disk that maps to the root web URI ('/')
     */
    BaseHandler(String webRoot) {
        if (webRoot == null) {
            throw new NullPointerException();
        }
        this.webRoot = webRoot;
    }

    /**
     * Provides a mapping to a location on disk based on a web request URI.
     * If the requested URI ends with '/' and the location contains an index.html
     * this method will send a redirect to the client to serve the index.html,
     * however this is only done for get requests.
     * @param ctx The Channel Handler Context.
     * @param uri The requested URI, must start with '/'.
     * @param isPost Pass in{@code true} is this is a post request, {@code false} otherwise.
     * @return The path on disk for the resource, or {@code null} if an error or redirect
     * has occurred. In that case the response has been written to the {@code ctx} argument.
     */
    String getPathFromUri(ChannelHandlerContext ctx, String uri, boolean isPost) {
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

        String path = webRoot + uri.replace('/', File.separatorChar);
        if (uri.trim().endsWith("/") && !isPost) {
            // Serve the index.html file if it exists in the directory
            if (new File(path + "index.html").isFile()) {
                sendRedirect(ctx, uri + "index.html");
                return null;
            }
        }

        return path;
    }

    /**
     * Send a directory listing rendered as HTML to the client.
     * @param ctx The Channel Handler Context.
     * @param uri The URI requested by the client.
     * @param directory The corresponding directory on disk.
     */
    static void sendDirectoryListing(ChannelHandlerContext ctx, String uri, File directory) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");

        String directoryHtml = DirectoryRenderer.renderDirectoryHTML(uri, directory);
        ByteBuf buffer = Unpooled.copiedBuffer(directoryHtml, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Send an error response to the client.
     * @param ctx The Channel Handler Context.
     * @param status The HTTP response status.
     */
    static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendError(ctx, status, "");
    }

    /**
     * Send an error response to the client with an additional informational message.
     * @param ctx The Channel Handler Context.
     * @param status The HTTP response status.
     * @param message The additional information to be presented to the client.
     */
    static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status.toString() + "\n" + message + "\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Send a HTTP redirect to the client.
     * @param ctx The Channel Handler Context.
     * @param newLocation The new location the client should navigate to.
     */
    static void sendRedirect(ChannelHandlerContext ctx, String newLocation) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaders.Names.LOCATION, newLocation);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
