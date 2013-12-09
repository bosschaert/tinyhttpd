package org.coderthoughts.tinyhttpd.itests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class to make it really easy to read an input stream.
 */
public class Streams {
    private Streams() {}

    /**
     * Copy the complete contents of the InputStream into an OutputStream
     * @param is The input stream to read from.
     * @param os The output stream to write to.
     * @throws IOException When an error occurs during reading or writing.
     */
    public static void pump(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[8192];

        int length = 0;
        int offset = 0;

        while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
            offset += length;

            if (offset == bytes.length) {
                os.write(bytes, 0, bytes.length);
                offset = 0;
            }
        }
        if (offset != 0) {
            os.write(bytes, 0, offset);
        }
    }

    /**
     * Read the complete contents of an InputStream into a byte array.
     * @param is The input stream to read from.
     * @return The contents of the stream in a byte array.
     * @throws IOException When an error occurs during reading.
     */
    public static byte [] suck(InputStream is) throws IOException {
        // Use the Java7 try-with-resources auto close on the output stream.
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            pump(is, baos);
            return baos.toByteArray();
        }
    }
}
