package com.yashgamerx.flcd.common.visual;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/// Saves a complete picture of a tree drawing canvas as a PNG, independent of
/// how the user has zoomed or panned it, and independent of the canvas's
/// own size.
///
/// What it captures: the union of everything drawn on the canvas (edges,
/// nodes, open info/name panels) plus a small margin, at `scale` times the
/// original 1:1 size. The white virtual canvas around the tree is cropped
/// away, and a tree that spills outside the canvas bounds is still captured
/// in full because the capture region is given explicitly.
///
/// How it stays safe for very large trees: the region is rendered in tiles
/// no bigger than [#MAX_TILE_SIDE] on a side (so no single GPU texture is
/// oversized), and finished rows are streamed straight into a
/// [PngRowWriter] so memory use is bounded by one strip, not the full image.
///
/// Must be called on the JavaFX Application Thread.
public final class CanvasSnapshotExporter {

    /// Largest width/height rendered in a single snapshot call.
    private static final int MAX_TILE_SIDE = 4096;
    /// Upper bound on the pixels held in memory at once (one strip of rows).
    private static final long MAX_STRIP_PIXELS = 16L * 1024 * 1024;
    /// White margin around the content, in canvas units (before scaling).
    private static final double MARGIN = 16.0;

    private CanvasSnapshotExporter() {
    }

    /// Writes `canvas` to `target` as a PNG at `scale` times its original size.
    public static void exportPng(Pane canvas, File target, double scale) throws IOException {
        if (scale <= 0) throw new IllegalArgumentException("scale must be positive");

        // Remember the live view state so it can be put back exactly as it was.
        double oldScaleX = canvas.getScaleX();
        double oldScaleY = canvas.getScaleY();
        double oldTranslateX = canvas.getTranslateX();
        double oldTranslateY = canvas.getTranslateY();
        try {
            // Capture at the original 1:1 size, not at the current zoom/pan.
            canvas.setScaleX(1.0);
            canvas.setScaleY(1.0);
            canvas.setTranslateX(0);
            canvas.setTranslateY(0);
            // Name panels counter-scale themselves to stay readable while zoomed;
            // at 1:1 they must be unscaled too or they would appear magnified.
            setNamePanelZoom(canvas, 1.0);

            Bounds content = contentBounds(canvas);
            if (content == null) {
                throw new IOException("There is nothing on the canvas to capture.");
            }
            writeTiled(canvas, target, content, scale);
        } finally {
            setNamePanelZoom(canvas, oldScaleX);
            canvas.setScaleX(oldScaleX);
            canvas.setScaleY(oldScaleY);
            canvas.setTranslateX(oldTranslateX);
            canvas.setTranslateY(oldTranslateY);
        }
    }

    /// Returns `file` with a `.png` extension added if it has none.
    public static File withPngExtension(File file) {
        return file.getName().toLowerCase().endsWith(".png")
                ? file
                : new File(file.getParentFile(), file.getName() + ".png");
    }

    private static void writeTiled(Pane canvas, File target, Bounds content, double scale) throws IOException {
        // Output pixel space = canvas space * scale; origin is the padded top-left corner.
        double originX = (content.getMinX() - MARGIN) * scale;
        double originY = (content.getMinY() - MARGIN) * scale;
        int outWidth = (int) Math.ceil((content.getWidth() + 2 * MARGIN) * scale);
        int outHeight = (int) Math.ceil((content.getHeight() + 2 * MARGIN) * scale);

        var params = new SnapshotParameters();
        params.setFill(Color.WHITE); // also paints the area outside the canvas's own background
        params.setTransform(Transform.scale(scale, scale));

        int stripHeight = (int) Math.max(1, Math.min(MAX_TILE_SIDE, MAX_STRIP_PIXELS / outWidth));
        int[] strip = new int[outWidth * stripHeight];

        try (var png = new PngRowWriter(new BufferedOutputStream(new FileOutputStream(target), 1 << 16),
                outWidth, outHeight)) {
            for (int y0 = 0; y0 < outHeight; y0 += stripHeight) {
                int h = Math.min(stripHeight, outHeight - y0);
                for (int x0 = 0; x0 < outWidth; x0 += MAX_TILE_SIDE) {
                    int w = Math.min(MAX_TILE_SIDE, outWidth - x0);
                    params.setViewport(new Rectangle2D(originX + x0, originY + y0, w, h));
                    var tile = canvas.snapshot(params, new WritableImage(w, h));
                    tile.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), strip, x0, outWidth);
                }
                for (int row = 0; row < h; row++) {
                    png.writeRow(strip, row * outWidth);
                }
            }
        }
    }

    /// Union of every child's bounds in canvas coordinates, ignoring the
    /// canvas's own (large, mostly empty) background area.
    private static Bounds contentBounds(Pane canvas) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        boolean any = false;
        for (Node child : canvas.getChildren()) {
            if (!child.isVisible()) continue;
            Bounds b = child.getBoundsInParent();
            if (b.isEmpty()) continue;
            any = true;
            minX = Math.min(minX, b.getMinX());
            minY = Math.min(minY, b.getMinY());
            maxX = Math.max(maxX, b.getMaxX());
            maxY = Math.max(maxY, b.getMaxY());
        }
        return any ? new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY) : null;
    }

    private static void setNamePanelZoom(Pane canvas, double canvasScale) {
        for (Node child : canvas.getChildren()) {
            if (child instanceof NodeNamePanel panel) {
                panel.compensateForZoom(canvasScale);
            }
        }
    }
}
