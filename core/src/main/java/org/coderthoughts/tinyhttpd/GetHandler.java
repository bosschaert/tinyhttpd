package org.coderthoughts.tinyhttpd;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

class GetHandler extends BaseHandler {
    private static final int HTTP_CACHE_SECONDS = 60;
    private static final SimpleDateFormat HTTP_DATE_FORMATTER;
    static {
        HTTP_DATE_FORMATTER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        HTTP_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    GetHandler(String webRoot) {
        super(webRoot);
    }

    void handleGetRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws ParseException, IOException {
        String path = getPathFromUri(ctx, request.getUri());
        if (path == null) {
            // the getPathFromUri method will have written the appropriate error response
            return;
        }

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            if (request.getUri().endsWith("/")) {
                sendDirectoryListing(ctx, request.getUri(), file);
            } else {
                sendRedirect(ctx, request.getUri() + "/");
            }
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        // Cache Validation
        String ifModifiedSince = request.headers().get(HttpHeaders.Names.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            Date ifModifiedSinceDate = HTTP_DATE_FORMATTER.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return;
            }
        }

        final RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpHeaders.setContentLength(response, raf.length());

        setMimeTypeHeader(response, file);
        setDateAndCacheHeaders(response, file);
        if (HttpHeaders.isKeepAlive(request)) {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        ctx.write(response);
        ChannelFuture sendFileFuture = ctx.write(
                new DefaultFileRegion(raf.getChannel(), 0, raf.length()), ctx.newProgressivePromise());
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationComplete(ChannelProgressiveFuture arg0) throws Exception {
                raf.close();
            }

            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                // Nothing to do
            }
        });

        // This writes the end marker
        ChannelFuture endMarkerFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if (!HttpHeaders.isKeepAlive(request)) {
            endMarkerFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);

        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaders.Names.DATE, HTTP_DATE_FORMATTER.format(time.getTime()));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaders.Names.DATE, HTTP_DATE_FORMATTER.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaders.Names.EXPIRES, HTTP_DATE_FORMATTER.format(time.getTime()));
        response.headers().set(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaders.Names.LAST_MODIFIED,
                HTTP_DATE_FORMATTER.format(new Date(fileToCache.lastModified())));
    }

    private static void setMimeTypeHeader(HttpResponse response, File file) throws IOException {
        // It would have been better to use Files.probeContentType(file.toPath()) but unfortunately
        // that doesn't work on Mac OSX.
        String mimeType = URLConnection.guessContentTypeFromName(file.getAbsolutePath());
        if (mimeType != null) {
            response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mimeType);
        }
    }
}
