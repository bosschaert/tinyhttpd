package org.coderthoughts.tinyhttpd;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

class DirectoryRenderer {
    private DirectoryRenderer() {
        // Do not instantiate
    }

    static String renderDirectoryHTML(String uri, File directory) {
        String trimmedURI = trimSlashes(uri);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Directory: ");
        sb.append(trimmedURI);
        sb.append("</title></head><body>");
        sb.append("<h1>Directory: ");
        sb.append(trimmedURI);
        sb.append("</h1>");
        sb.append("<form action=\"/upload_file\" method=\"POST\">Upload a file to this directory:");
        sb.append("<input type=\"FILE\" name=\"file\"/>");
        sb.append("<input type=\"SUBMIT\" name=\"upload\" value=\"Upload File\">");
        sb.append("</form>");
        sb.append("<table>");

        if (!uri.trim().equals("/")) {
            // we're not at the top yet, provide a link to the parent directory
            sb.append("<tr><td><a href=\"");
            sb.append(uri);
            sb.append("..\">parent directory</a></td><td></td></tr>");
        }

        File[] files = directory.listFiles();
        Arrays.sort(files, new SortingFileComparator());
        renderFiles(sb, uri, files);

        sb.append("</table></body></html>\n");

        return sb.toString();
    }

    private static String trimSlashes(String s) {
        s = s.trim();
        if (s.equals("/"))
            return s;

        if (s.startsWith("/"))
            s = s.substring(1);

        if (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);

        return s;
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
