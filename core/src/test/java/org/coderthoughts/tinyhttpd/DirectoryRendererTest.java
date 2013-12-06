package org.coderthoughts.tinyhttpd;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class DirectoryRendererTest {
    @Test
    public void testDirectoryRendering() {
        File dir1 = new File(getClass().getResource("/dir1").getFile());
        String html = DirectoryRenderer.renderDirectoryHTML("/dir1/", dir1);
        Assert.assertTrue(html.contains("blah.txt"));
        Assert.assertTrue(html.contains("subdir"));
    }
}
