package org.coderthoughts.tinyhttpd;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class DirectoryRendererTest {
    @Test
    public void testDirectoryRendering() {
        File dir1 = new File(getClass().getResource("/dir1").getFile());
        String html = DirectoryRenderer.renderDirectoryHTML("/dir1/", dir1);

        int parentDirIdx = html.indexOf("<a href=\"/dir1/..\">parent directory</a>");
        int subDirIdx = html.indexOf("<a href=\"/dir1/subdir\">subdir</a>");
        int fileIdx = html.indexOf("<a href=\"/dir1/blah.txt\">blah.txt</a>");

        Assert.assertTrue(parentDirIdx > 0);
        Assert.assertTrue(fileIdx > 0);
        Assert.assertTrue(subDirIdx > 0);
        Assert.assertTrue("Parent directory should be listed before other directories", parentDirIdx < subDirIdx);
        Assert.assertTrue("Subdir should be listed before the file", subDirIdx < fileIdx);
    }

    @Test
    public void testRootDirectoryRendering() {
        File dir1 = new File(getClass().getResource("/").getFile());
        String html = DirectoryRenderer.renderDirectoryHTML("/", dir1);

        int parentDirIdx = html.indexOf("parent directory");
        Assert.assertEquals("Should not show a parent directory for the root", -1, parentDirIdx);
        int dir1Idx = html.indexOf("<a href=\"/dir1\">dir1</a>");
        Assert.assertTrue("Dir 1 should be listed", dir1Idx > 0);
    }
}
