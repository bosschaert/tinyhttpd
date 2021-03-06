package org.coderthoughts.tinyhttpd;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Handler for HTTP POST requests. The only POST requests supported are file upload
 * requests. To upload a file to a directory, send the post request to the URI representing
 * the directory.
 */
class PostHandler extends BaseHandler {
    private static HttpDataFactory factory = new DefaultHttpDataFactory(true);

    // This is fairly standard Netty configuration
    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
    }

    /**
     * Constructor. The web-root location must be provided.
     * @param webRoot The location on disk that maps to the root web URI ('/')
     */
    PostHandler(String webRoot) {
        super(webRoot);
    }

    /**
     * Handle the POST request.
     * @param ctx The Channel Handler Context from Netty.
     * @param request The POST request.
     */
    void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.getUri();

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, request);
        decoder.offer(request);

        String path = getPathFromUri(ctx, uri, true);
        if (path == null) {
            return;
        }

        File directory = new File(path);
        readHttpDataChunkByChunk(ctx, decoder, directory);

        if (request instanceof LastHttpContent) {
            sendDirectoryListing(ctx, uri, directory);
            decoder.destroy();
        }
    }

    /**
     * Read the HTTP chunks.
     */
    private void readHttpDataChunkByChunk(ChannelHandlerContext ctx, HttpPostRequestDecoder decoder, File targetDir) {
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        writeHttpData(ctx, data, targetDir);
                    } finally {
                        data.release();
                    }
                }
            }
        } catch (EndOfDataDecoderException e) {
            // There is no more data, strangely enough Netty can signal this by sending an Exception
        }
    }

    /**
     * Write the uploaded file to disk. If a file with this name already exists in the upload location
     * an HTTP 406 error code is returned.
     * @param ctx The Channel Handler Context.
     * @param data The data provided by the HTTP request.
     * @param uploadDir The directory on disk where the upload needs to go to.
     */
    private void writeHttpData(ChannelHandlerContext ctx, InterfaceHttpData data, File uploadDir) {
        try {
            if (data.getHttpDataType() == HttpDataType.FileUpload) {
                FileUpload fileUpload = (FileUpload) data;
                if (fileUpload.isCompleted()) {
                    File uploadedFile = new File(uploadDir, fileUpload.getFilename());
                    if (uploadedFile.exists()) {
                        sendError(ctx, HttpResponseStatus.NOT_ACCEPTABLE,
                                "File already exists: " + fileUpload.getFilename());
                        return;
                    }

                    // Use the Java7 try-with-resources auto close on the output stream.
                    try (FileOutputStream fos = new FileOutputStream(uploadedFile)) {
                        fileUpload.getByteBuf().getBytes(0, fos, (int) fileUpload.length());
                    }
                }
            }
        } catch (IOException e) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
