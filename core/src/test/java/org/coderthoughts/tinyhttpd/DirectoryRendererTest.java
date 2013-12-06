package org.coderthoughts.tinyhttpd;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class DirectoryRendererTest {
    @Test
    public void testDirectoryRendering() {
        File dir1 = new File(getClass().getResource("/dir1").getFile());
        String html = DirectoryRenderer.renderDirectoryHTML("/dir1/", dir1);

        int fileIdx = html.indexOf("blah.txt");
        int subDirIdx = html.indexOf("subdir");

        Assert.assertTrue(fileIdx > 0);
        Assert.assertTrue(subDirIdx > 0);
        Assert.assertTrue("Subdir should be listed before the file", subDirIdx < fileIdx);
    }
}
