package org.coderthoughts.tinyhttpd;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class DirectoryRendererTest {
    @Test
    public void testDirectoryRendering() {
        File dir1 = new File(getClass().getResource("/dir1").getFile());
        String html = DirectoryRenderer.renderDirectoryHTML("/dir1/", dir1);

        int fileIdx = html.indexOf("<a href=\"/dir1/blah.txt\">blah.txt</a>");
        int subDirIdx = html.indexOf("<a href=\"/dir1/subdir\">subdir</a>");

        Assert.assertTrue(fileIdx > 0);
        Assert.assertTrue(subDirIdx > 0);
        Assert.assertTrue("Subdir should be listed before the file", subDirIdx < fileIdx);
    }
}
