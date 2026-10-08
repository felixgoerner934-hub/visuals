package dev.lumina.gui;

/** Small colour helpers. All "argb" values are 0xAARRGGBB. */
public final class Colors {
    private Colors() {}

    public static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

    /** Combines an RGB colour with an alpha in the range 0..1. */
    public static int alpha(int rgb, float a) {
        return (Math.round(clamp01(a) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    /** Multiplies the alpha channel of an ARGB colour. */
    public static int mulAlpha(int argb, float f) {
        int a = Math.round(((argb >>> 24) & 0xFF) * clamp01(f));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = clamp01(t);
        int a = (int) (((c1 >>> 24) & 0xFF) + (((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float[] rgbToHsb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0f;
        if (delta > 0f) {
            if (max == r) h = ((g - b) / delta) % 6f;
            else if (max == g) h = (b - r) / delta + 2f;
            else h = (r - g) / delta + 4f;
            h /= 6f;
            if (h < 0f) h += 1f;
        }
        float s = max == 0f ? 0f : delta / max;
        return new float[]{h, s, max};
    }

    public static int hsbToRgb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        s = clamp01(s);
        v = clamp01(v);
        float hh = h * 6f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }
}
