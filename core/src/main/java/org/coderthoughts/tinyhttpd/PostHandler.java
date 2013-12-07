package org.coderthoughts.tinyhttpd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
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
import io.netty.util.CharsetUtil;

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
        String targetDirectory = request.getUri();

        decoder = new HttpPostRequestDecoder(factory, request);

        chunked = HttpHeaders.isTransferEncodingChunked(request);
        responseContent.append("Is Chunked: " + chunked + "\n");
        responseContent.append("Is Multipart: " + decoder.isMultipart() + "\n");
        if (chunked) {
            responseContent.append("Chunks: ");
            readingChunks = true;
        }

        decoder.offer(request);
        readHttpDataChunkByChunk(ctx, targetDirectory);

        if (request instanceof LastHttpContent) {
            // TODO writeResponse(ctx.channel());
            ByteBuf buffer = Unpooled.copiedBuffer(responseContent.toString(), CharsetUtil.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
            response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

            boolean close = request.headers().contains(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE, true)
                    || request.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
                    && !request.headers().contains(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE, true);
            if (!close) {
                response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
            }
            ChannelFuture future = ctx.writeAndFlush(response);
            if (close) {
                future.addListener(ChannelFutureListener.CLOSE);
            }

            readingChunks = false;
            responseContent.setLength(0);
            decoder.destroy();
            decoder = null;
        }
    }


    private void readHttpDataChunkByChunk(ChannelHandlerContext ctx, String uriPath) {
        String path = getPathFromUri(ctx, uriPath);
        if (path == null) {
            return;
        }
        File targetDir = new File(path);

        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        writeHttpData(data, targetDir);
                    } finally {
                        data.release();
                    }
                }
            }
        } catch (EndOfDataDecoderException e) {
            // There is no more data, strangely enough Netty signals this by sending an Exception
        }
    }

    private void writeHttpData(InterfaceHttpData data, File uploadDir) {
        try {
            if (data.getHttpDataType() == HttpDataType.FileUpload) {
                FileUpload fileUpload = (FileUpload) data;
                if (fileUpload.isCompleted()) {
                    File uploadedFile = new File(uploadDir, fileUpload.getFilename());
                    if (uploadedFile.exists()) {
                        // TODO send an error
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
            // TODO Auto-generated catch block
            e.printStackTrace();
            // TODO senderror
        }
        /*
        if (data.getHttpDataType() == HttpDataType.Attribute) {
            Attribute attr = (Attribute) data;
            try {
                responseContent.append("\n BODY attribute: " + attr.getHttpDataType().name() + ":" + attr.getName() + "=" + attr.getValue());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            responseContent.append("\nBODY FileUpload: " + data.getHttpDataType().name() + ":" + data.toString());
            if (data.getHttpDataType() == HttpDataType.FileUpload) {
                FileUpload fileUpload = (FileUpload) data;
                if (fileUpload.isCompleted()) {
                    responseContent.append("\nContent of file:\n");
                    try {
                        responseContent.append(fileUpload.getString(fileUpload.getCharset()));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        */
    }
}
