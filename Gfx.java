package dev.lumina.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Immediate-mode drawing helpers (rounded rectangles, shadows, text). */
public final class Gfx {
    /** Global alpha multiplier, used for fade in/out of the whole menu. */
    public static float ga = 1f;

    private static final int MAX_R = 16;
    private static final int[][] INSETS = new int[MAX_R + 1][];

    static {
        for (int r = 1; r <= MAX_R; r++) {
            int[] row = new int[r];
            for (int i = 0; i < r; i++) {
                double dy = r - i - 0.5;
                row[i] = r - (int) Math.round(Math.sqrt(r * (double) r - dy * dy));
            }
            INSETS[r] = row;
        }
    }

    private Gfx() {}

    public static void fill(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
        argb = Colors.mulAlpha(argb, ga);
        if ((argb >>> 24) < 3 || w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, argb);
    }

    public static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int argb) {
        argb = Colors.mulAlpha(argb, ga);
        if ((argb >>> 24) < 3 || w <= 0 || h <= 0) return;
        r = Math.min(Math.min(r, MAX_R), Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, argb);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, argb);
        int[] ins = INSETS[r];
        for (int i = 0; i < r; i++) {
            g.fill(x + ins[i], y + i, x + w - ins[i], y + i + 1, argb);
            g.fill(x + ins[i], y + h - 1 - i, x + w - ins[i], y + h - i, argb);
        }
    }

    /** 1px square outline (four thin rectangles). */
    public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
        fill(g, x, y, w, 1, argb);
        fill(g, x, y + h - 1, w, 1, argb);
        fill(g, x, y + 1, 1, h - 2, argb);
        fill(g, x + w - 1, y + 1, 1, h - 2, argb);
    }

    /** 1px rounded outline (drawn as two rounded rects). */
    public static void roundOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int outline, int inner) {
        round(g, x, y, w, h, r, outline);
        round(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), inner);
    }

    /** Soft shadow made of a few expanding translucent rounded rects. */
    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, float strength) {
        if (strength <= 0.01f) return;
        for (int i = 5; i >= 1; i--) {
            float a = strength * 0.075f * (6 - i) / 5f;
            round(g, x - i * 2, y - i * 2 + 4, w + i * 4, h + i * 4, r + i * 2, Colors.alpha(0x000000, a));
        }
    }

    public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int argb, boolean shadow) {
        argb = Colors.mulAlpha(argb, ga);
        // Vanilla treats very low alpha as opaque, so skip nearly invisible text.
        if ((argb >>> 24) < 12 || s.isEmpty()) return;
        g.text(f, s, x, y, argb, shadow);
    }

    public static void textRight(GuiGraphicsExtractor g, Font f, String s, int right, int y, int argb, boolean shadow) {
        text(g, f, s, right - f.width(s), y, argb, shadow);
    }

    public static void textCenter(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int argb, boolean shadow) {
        text(g, f, s, cx - f.width(s) / 2, y, argb, shadow);
    }

    /** Shortens a string with "..." so it fits into the given pixel width. */
    public static String fit(Font f, String s, int maxW) {
        if (maxW <= 0) return "";
        if (f.width(s) <= maxW) return s;
        int w3 = f.width("...");
        int end = s.length();
        while (end > 0 && f.width(s.substring(0, end)) + w3 > maxW) end--;
        return s.substring(0, end) + "...";
    }
}
