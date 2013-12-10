package org.coderthoughts.tinyhttpd;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * The DirectoryRenderer renders a HTML page displaying a directory. It also contains a form
 * for uploading files to this directory.
 * Files and sub-directories are rendered with links so that the user can click on them to see their contents.
 */
class DirectoryRenderer {
    private DirectoryRenderer() {
        // Do not instantiate
    }

    /**
     * Renders the directory listing HTML page, including the upload form. If we are not at the top URI yet
     * the page will contain a link to navigate to the parent directory. Contents are sorted: sub-directories
     * first, after that files are listed in alphabetical order.
     * @param uri The URI as requested by the client.
     * @param directory The directory on disk that corresponds to the URI.
     * @return The HTML page, ready to be returned to the client.
     */
    static String renderDirectoryHTML(String uri, File directory) {
        String trimmedURI = trimSlashes(uri);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Directory: ");
        sb.append(trimmedURI);
        sb.append("</title><style type=\"text/css\">");
        sb.append("body {font-family:sans-serif; background-color:#FFFFEB;}");
        sb.append("td {padding-left:15px;}");
        sb.append("</style></head><body>");
        sb.append("<h1>Directory: ");
        sb.append(trimmedURI);
        sb.append("</h1>");
        sb.append("<form action=\"");
        sb.append(uri);
        sb.append("\" enctype=\"multipart/form-data\" method=\"POST\">");
        sb.append("Upload a file to this directory: ");
        sb.append("<input type=\"FILE\" name=\"file\"/>");
        sb.append("<input type=\"SUBMIT\" name=\"upload\" value=\"Upload File\">");
        sb.append(" (max size ");
        sb.append(ServerController.MAX_HTTP_CONTENT_LENGTH / (1024 * 1024));
        sb.append("mb)</form><p/>");
        sb.append("<table>");

        if (!uri.trim().equals("/")) {
            // we're not at the top yet, provide a link to the parent directory
            sb.append("<tr><td><b><a href=\"");
            sb.append(uri);
            sb.append("..\">parent directory</a></b></td><td></td></tr>");
        }

        File[] files = directory.listFiles();
        Arrays.sort(files, new SortingFileComparator());
        renderFiles(sb, uri, files);

        sb.append("</table></body></html>\n");

        return sb.toString();
    }

    /**
     * Remove leading slash and/or trailing slash. Except when the only character is a slash.
     * @param s The string to trim slashes on.
     * @return The string, with slashes trimmed, or '/' if that was the input.
     */
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

    /**
     * Render HTML for the files provided, which could be either files or directories.
     * @param sb The StringBuilder used to build the HTML page.
     * @param baseUri The base URI as requested by the client.
     * @param files The file listing of the current directory to render.
     */
    private static void renderFiles(StringBuilder sb, String baseUri, File[] files) {
        for (File f : files) {
            sb.append("<tr><td>");
            if (f.isDirectory()) {
                sb.append("<b>");
            }
            sb.append("<a href=\"");
            sb.append(baseUri);
            sb.append(f.getName());
            sb.append("\">");
            sb.append(f.getName());
            sb.append("</a>");
            if (f.isDirectory()) {
                sb.append("</b>");
            }
            sb.append("</td><td>");
            if (f.isDirectory()) {
                sb.append("directory");
            } else {
                sb.append(f.length());
                sb.append(" bytes");
            }
            sb.append("</td></tr>");
        }
    }

    /**
     * A comparator that sorts directories before files. Both directories and files are
     * alphabetically sorted themselves.
     */
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
