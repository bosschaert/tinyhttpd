package org.coderthoughts.tinyhttpd;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class BaseHandlerTest {
    @Test
    public void testGetSimplePathFromUri() {
        BaseHandler bh = new BaseHandler("/foo") {};

        String path = bh.getPathFromUri(null, "/bar/", false);
        Assert.assertEquals("/foo/bar/", path);
    }

    @Test
    public void testGetPathFromUriDoesNotStartWithSlash() {
        ChannelHandlerContext mockCtx = Mockito.mock(ChannelHandlerContext.class);
        Mockito.when(mockCtx.writeAndFlush(Mockito.any())).thenReturn(Mockito.mock(ChannelFuture.class));

        BaseHandler bh = new BaseHandler("/foo") {};
        Assert.assertNull(bh.getPathFromUri(mockCtx, "bar/", false));

        Mockito.verify(mockCtx).writeAndFlush(Mockito.argThat(new HttpResponseCodeMatcher(403)));
    }

    @Test
    public void testGetPathRedirectToIndexHtml() {
        File dir2 = new File(getClass().getResource("/dir2").getFile());

        ChannelHandlerContext mockCtx = Mockito.mock(ChannelHandlerContext.class);
        Mockito.when(mockCtx.writeAndFlush(Mockito.any())).thenReturn(Mockito.mock(ChannelFuture.class));

        BaseHandler bh = new BaseHandler(dir2.getAbsolutePath()) {};
        Assert.assertNull(bh.getPathFromUri(mockCtx, "/", false));

        Map<String, String> headers = new HashMap<>();
        headers.put("Location", "/index.html");
        Mockito.verify(mockCtx).writeAndFlush(Mockito.argThat(new HttpResponseCodeMatcher(302, headers)));
    }

    static class HttpResponseCodeMatcher extends TypeSafeMatcher<HttpResponse> {
        private final int responseCode;
        private final Map<String, String> responseHeaders;

        HttpResponseCodeMatcher(int code) {
            this(code, Collections.<String,String>emptyMap());
        }

        HttpResponseCodeMatcher(int code, Map<String,String> expectedHeaders) {
            responseCode = code;
            responseHeaders = expectedHeaders;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Invalid HTTP response, expected: " + responseCode +
                    " with headers: " + responseHeaders);
        }

        @Override
        protected boolean matchesSafely(HttpResponse item) {
            for (Map.Entry<String,String> entry : responseHeaders.entrySet()) {
                if (!item.headers().get(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
            }

            return item.getStatus().code() == responseCode;
        }
    };
}
