package org.coderthoughts.tinyhttpd;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
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

class PostHandler extends BaseHandler {
    private static HttpDataFactory factory = new DefaultHttpDataFactory(true);
    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
    }

    // TODO remove these
    StringBuilder responseContent = new StringBuilder();
    HttpPostRequestDecoder decoder;
    boolean chunked = false;
    boolean readingChunks = false;

    public PostHandler(String webRoot) {
        super(webRoot);
    }

    void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.getUri();

        decoder = new HttpPostRequestDecoder(factory, request);

        chunked = HttpHeaders.isTransferEncodingChunked(request);
        responseContent.append("Is Chunked: " + chunked + "\n");
        responseContent.append("Is Multipart: " + decoder.isMultipart() + "\n");
        if (chunked) {
            responseContent.append("Chunks: ");
            readingChunks = true;
        }

        decoder.offer(request);

        String path = getPathFromUri(ctx, uri);
        if (path == null) {
            return;
        }
        File directory = new File(path);

        readHttpDataChunkByChunk(ctx, directory);

        if (request instanceof LastHttpContent) {
            sendDirectoryListing(ctx, uri, directory);

            readingChunks = false;
            responseContent.setLength(0);
            decoder.destroy();
            decoder = null;
        }
    }


    private void readHttpDataChunkByChunk(ChannelHandlerContext ctx, File targetDir) {
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
            // There is no more data, strangely enough Netty signals this by sending an Exception
        }
    }

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
                    FileOutputStream fos = new FileOutputStream(uploadedFile);
                    try {
                        fileUpload.getByteBuf().getBytes(0, fos, (int) fileUpload.length());
                    } finally {
                        fos.close();
                    }
                }
            }
        } catch (IOException e) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
