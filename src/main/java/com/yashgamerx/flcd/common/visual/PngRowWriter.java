package com.yashgamerx.flcd.common.visual;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/// Minimal streaming PNG encoder (8-bit RGB, no alpha). Rows are pushed one
/// at a time and compressed immediately, so an image of any size can be
/// written without ever holding the whole bitmap in memory. This is what
/// lets the snapshot exporter capture trees far larger than a single GPU
/// texture or a `BufferedImage` would allow, and it needs no extra JavaFX
/// module (no `javafx.swing` / `ImageIO`).
public final class PngRowWriter implements Closeable {

    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final DataOutputStream out;
    private final IdatChunkStream idat;
    private final DeflaterOutputStream deflater;
    private final byte[] rowBuffer;
    private final int width;
    private final int height;
    private int rowsWritten;
    private boolean closed;

    public PngRowWriter(OutputStream target, int width, int height) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image size must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.out = new DataOutputStream(target);
        this.rowBuffer = new byte[1 + width * 3]; // filter byte + RGB triples

        out.write(SIGNATURE);
        writeHeaderChunk();

        this.idat = new IdatChunkStream(out);
        this.deflater = new DeflaterOutputStream(idat, new Deflater(Deflater.DEFAULT_COMPRESSION));
    }

    private static void writeChunk(DataOutputStream out, String type, byte[] data, int off, int len)
            throws IOException {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data, off, len);
        out.writeInt(len);
        out.write(typeBytes);
        out.write(data, off, len);
        out.writeInt((int) crc.getValue());
    }

    /// Writes one row from packed ARGB ints (alpha ignored), starting at `offset`.
    public void writeRow(int[] argb, int offset) throws IOException {
        if (rowsWritten >= height) {
            throw new IllegalStateException("All " + height + " rows were already written.");
        }
        int p = 1; // rowBuffer[0] stays 0 = filter type "None"
        for (int x = 0; x < width; x++) {
            int pixel = argb[offset + x];
            rowBuffer[p++] = (byte) (pixel >> 16);
            rowBuffer[p++] = (byte) (pixel >> 8);
            rowBuffer[p++] = (byte) pixel;
        }
        deflater.write(rowBuffer);
        rowsWritten++;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            if (rowsWritten != height) {
                throw new IOException("PNG incomplete: wrote " + rowsWritten + " of " + height + " rows.");
            }
            deflater.finish();
            idat.flushChunk();
            writeChunk(out, "IEND", new byte[0], 0, 0);
            out.flush();
        } finally {
            out.close();
        }
    }

    private void writeHeaderChunk() throws IOException {
        var header = new java.io.ByteArrayOutputStream(13);
        var data = new DataOutputStream(header);
        data.writeInt(width);
        data.writeInt(height);
        data.writeByte(8); // bit depth
        data.writeByte(2); // color type: truecolor RGB
        data.writeByte(0); // compression: deflate
        data.writeByte(0); // filter method
        data.writeByte(0); // interlace: none
        byte[] bytes = header.toByteArray();
        writeChunk(out, "IHDR", bytes, 0, bytes.length);
    }

    /// Collects compressed bytes and emits them as a series of IDAT chunks.
    private static final class IdatChunkStream extends OutputStream {
        private final DataOutputStream out;
        private final byte[] buffer = new byte[64 * 1024];
        private int length;

        IdatChunkStream(DataOutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] bytes, int off, int len) throws IOException {
            while (len > 0) {
                int n = Math.min(len, buffer.length - length);
                System.arraycopy(bytes, off, buffer, length, n);
                length += n;
                off += n;
                len -= n;
                if (length == buffer.length) flushChunk();
            }
        }

        void flushChunk() throws IOException {
            if (length > 0) {
                writeChunk(out, "IDAT", buffer, 0, length);
                length = 0;
            }
        }
    }
}
