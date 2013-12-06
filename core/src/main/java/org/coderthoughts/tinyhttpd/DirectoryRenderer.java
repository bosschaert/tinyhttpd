package org.coderthoughts.tinyhttpd;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

class DirectoryRenderer {
    private DirectoryRenderer() {
        // Do not instantiate
    }

    static String renderDirectoryHTML(String uri, File directory) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html><head><title>Directory: ");
        sb.append(uri);
        sb.append("</title></head><body>");
        sb.append("<h1>Directory: ");
        sb.append(uri);
        sb.append("</h1>");
        sb.append("<table>");

        File[] files = directory.listFiles();
        Arrays.sort(files, new SortingFileComparator());
        renderFiles(sb, uri, files);

        sb.append("</table></body></html>\n");

        return sb.toString();
    }

    private static void renderFiles(StringBuilder sb, String baseUri, File[] files) {
        for (File f : files) {
            sb.append("<tr><td><a href=\"");
            sb.append(baseUri);
            sb.append(f.getName());
            sb.append("\">");
            sb.append(f.getName());
            sb.append("</a></td><td>");
            if (f.isDirectory()) {
                sb.append("directory");
            } else {
                sb.append(f.length());
                sb.append(" bytes");
            }
            sb.append("</td></tr>");
        }
    }

    private static class SortingFileComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            if (f1.isDirectory()) {
                if (f2.isDirectory()) {
                    return f1.getName().compareTo(f2.getName());
                } else {
                    // Directories go before ordinary files
                    return -1;
                }
            } else {
                if (f2.isDirectory()) {
                    // Ordinary files go after directories
                    return 1;
                }
            }

            return f1.getName().compareTo(f2.getName());
        }
    }
}
