package org.coderthoughts.tinyhttpd;

import java.io.File;

class DirectoryRenderer {
    private DirectoryRenderer() {
        // Do not instantiate
    }

    static String renderDirectoryHTML(String uri, File directory) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html><head><title>Directory: " + uri);
        sb.append("</title></head>");
        sb.append("</html>\n");

        return sb.toString();
    }
}
