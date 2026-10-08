package dev.lumina.hud;

import dev.lumina.anim.Anim;
import dev.lumina.gui.Colors;
import dev.lumina.gui.LuminaScreen;
import dev.lumina.setting.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** Custom crosshair + hitmarker. Draws only filled rectangles, so it is very cheap. */
public final class Crosshair {
    /** Time of the last attack (System.nanoTime), written by the attack callback. */
    public static volatile long hitNanos = 0L;

    private static final Anim SPREAD = new Anim(0f, 18f);
    private static long lastFrame = System.nanoTime();

    // per-frame drawing state
    private static GuiGraphicsExtractor gfx;
    private static int pass;
    private static int fillColor;
    private static int outlineColor;
    private static boolean outline;

    private Crosshair() {}

    public static void markHit() {
        hitNanos = System.nanoTime();
    }

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null && !(mc.screen instanceof LuminaScreen)) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        // The vanilla crosshair is removed when this mod loads, so it is always drawn here
        // (the "Custom Crosshair" switch then picks the vanilla-like plain cross).
        boolean custom = Settings.MASTER.get() && Settings.CH_ENABLED.get();

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;

        gfx = g;
        if (!custom) {
            // vanilla look: white 1px cross with dark outline
            fillColor = 0xFFFFFFFF;
            outlineColor = 0xFF000000;
            outline = true;
            drawCross(cx, cy, 0, 5, 1, false);
            return;
        }

        boolean smooth = Settings.ANIM_HUD.get();
        int gap = Settings.CH_GAP.getI();
        if (Settings.CH_DYNAMIC.get()) {
            LocalPlayer p = mc.player;
            float amount = Settings.CH_DYNAMIC_AMOUNT.getF();
            double speed = p.getDeltaMovement().horizontalDistance();
            float target = 0f;
            if (speed > 0.04) target = amount * 0.6f;
            if (p.isSprinting()) target = amount;
            if (!p.onGround()) target = Math.max(target, amount * 0.8f);
            SPREAD.target = target;
            if (smooth) SPREAD.update(dt); else SPREAD.snap(target);
            gap += Math.round(SPREAD.value);
        }

        int rgb = Settings.CH_COLOR.get();
        if (Settings.CH_RAINBOW.get()) {
            float hue = (now / 1e9f) * Settings.CH_RAINBOW_SPEED.getF();
            rgb = Colors.hsbToRgb(hue, 0.8f, 1f);
        }
        float opacity = Settings.CH_OPACITY.getF();
        fillColor = Colors.alpha(rgb, opacity);
        outlineColor = Colors.alpha(Settings.CH_OUTLINE_COLOR.get(), opacity);
        outline = Settings.CH_OUTLINE.get() || Settings.HIGH_CONTRAST.get();

        int size = Settings.CH_SIZE.getI();
        int th = Settings.CH_THICKNESS.getI();
        switch (Settings.CH_STYLE.index()) {
            case 0 -> drawCross(cx, cy, gap, size, th, false);
            case 1 -> {
                drawCross(cx, cy, gap, size, th, false);
                drawDot(cx, cy, th);
            }
            case 2 -> drawDot(cx, cy, th);
            case 3 -> drawCircle(cx, cy, gap + size / 2 + 2, th);
            case 4 -> drawSquare(cx, cy, gap + size / 2 + 1, th);
            case 5 -> drawCross(cx, cy, gap, size, th, true);
            default -> drawX(cx, cy, gap, size, th);
        }

        if (Settings.HIT_ENABLED.get()) drawHitmarker(cx, cy, now, smooth);
    }

    // ------------------------------------------------------------------ primitives

    /** Draws a rectangle in the current pass: outline pass is 1px bigger and drawn first. */
    private static void rect(int x, int y, int w, int h) {
        if (pass == 0) {
            if (outline) gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, outlineColor);
        } else {
            gfx.fill(x, y, x + w, y + h, fillColor);
        }
    }

    private static void both(Runnable shape) {
        pass = 0;
        shape.run();
        pass = 1;
        shape.run();
    }

    private static void drawCross(int cx, int cy, int gap, int len, int th, boolean tShape) {
        final int a = th / 2;
        both(() -> {
            if (!tShape) rect(cx - a, cy - gap - len, th, len);        // top
            rect(cx - a, cy + gap + (th - a), th, len);                 // bottom
            rect(cx - gap - len, cy - a, len, th);                      // left
            rect(cx + gap + (th - a), cy - a, len, th);                 // right
        });
    }

    private static void drawDot(int cx, int cy, int th) {
        final int s = Math.max(2, th);
        both(() -> rect(cx - s / 2, cy - s / 2, s, s));
    }

    private static void drawSquare(int cx, int cy, int r, int th) {
        both(() -> {
            rect(cx - r, cy - r, 2 * r + th, th);
            rect(cx - r, cy + r, 2 * r + th, th);
            rect(cx - r, cy - r + th, th, 2 * r - th);
            rect(cx + r, cy - r + th, th, 2 * r - th);
        });
    }

    private static void drawCircle(int cx, int cy, int r, int th) {
        final int segs = switch (dev.lumina.setting.Settings.CIRCLE_QUALITY.index()) {
            case 0 -> 16;
            case 2 -> 48;
            default -> 28;
        };
        both(() -> {
            for (int i = 0; i < segs; i++) {
                double ang = (Math.PI * 2.0 * i) / segs;
                int px = cx + (int) Math.round(Math.cos(ang) * r) - th / 2;
                int py = cy + (int) Math.round(Math.sin(ang) * r) - th / 2;
                rect(px, py, th, th);
            }
        });
    }

    private static void drawX(int cx, int cy, int gap, int len, int th) {
        both(() -> {
            for (int i = 0; i < len; i++) {
                int d = gap + i;
                rect(cx - d - th / 2, cy - d - th / 2, th, th);
                rect(cx + d - th / 2, cy - d - th / 2, th, th);
                rect(cx - d - th / 2, cy + d - th / 2, th, th);
                rect(cx + d - th / 2, cy + d - th / 2, th, th);
            }
        });
    }

    private static void drawHitmarker(int cx, int cy, long now, boolean animate) {
        float ageMs = (now - hitNanos) / 1_000_000f;
        float dur = Settings.HIT_DURATION.getF();
        if (hitNanos == 0L || ageMs < 0f || ageMs > dur) return;
        float t = ageMs / dur;
        float fade = animate ? (1f - t) : 1f;
        int gap = 3 + (animate ? Math.round(t * 3f) : 0);
        int len = Settings.HIT_SIZE.getI();
        int color = Colors.alpha(Settings.HIT_COLOR.get(), fade);
        for (int i = 0; i < len; i++) {
            int d = gap + i;
            gfx.fill(cx - d, cy - d, cx - d + 1, cy - d + 1, color);
            gfx.fill(cx + d, cy - d, cx + d + 1, cy - d + 1, color);
            gfx.fill(cx - d, cy + d, cx - d + 1, cy + d + 1, color);
            gfx.fill(cx + d, cy + d, cx + d + 1, cy + d + 1, color);
        }
    }
}
