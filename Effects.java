package dev.lumina.hud;

import dev.lumina.anim.Anim;
import dev.lumina.gui.Colors;
import dev.lumina.setting.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Full-screen effects: tint, vignette, cinematic bars and the custom hurt flash. */
public final class Effects {
    private static final Anim BARS = new Anim(0f, 8f);
    private static long lastFrame = System.nanoTime();
    private static float lastHealth = -1f;
    private static long hurtNanos = 0L;

    private Effects() {}

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        // Track health even when the effect is off, so enabling it does not trigger a false flash.
        float health = mc.player.getHealth();
        if (lastHealth >= 0f && health < lastHealth - 0.01f) hurtNanos = now;
        lastHealth = health;

        if (!Settings.MASTER.get() || Settings.LITE.get()) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        if (Settings.FX_TINT.get()) {
            int c = Colors.alpha(Settings.FX_TINT_COLOR.get(), Settings.FX_TINT_OPACITY.getF());
            g.fill(0, 0, w, h, c);
        }
        if (Settings.FX_VIGNETTE.get()) {
            edges(g, w, h, Settings.FX_VIGNETTE_COLOR.get(), Settings.FX_VIGNETTE_STRENGTH.getF(), 0.16f);
        }
        if (Settings.FX_HURT.get() && hurtNanos != 0L) {
            float age = (now - hurtNanos) / 1_000_000f;
            float dur = Settings.FX_HURT_DURATION.getF();
            if (age >= 0f && age < dur) {
                float t = age / dur;
                float a = Settings.FX_HURT_INTENSITY.getF() * (1f - t) * (1f - t);
                edges(g, w, h, Settings.FX_HURT_COLOR.get(), a, 0.22f);
            }
        }

        BARS.target = Settings.FX_LETTERBOX.get() ? 1f : 0f;
        if (Settings.ANIM_HUD.get()) BARS.update(dt); else BARS.snap(BARS.target);
        if (BARS.value > 0.003f) {
            int bar = Math.round(h * Settings.FX_LETTERBOX_SIZE.getF() / 100f * BARS.value);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
    }

    /** Darkens/colours the four screen edges with a quadratic falloff (16 strips per side at most). */
    private static void edges(GuiGraphicsExtractor g, int w, int h, int rgb, float strength, float reach) {
        if (strength <= 0.01f) return;
        int steps = 12;
        int maxX = Math.round(w * reach);
        int maxY = Math.round(h * reach);
        for (int i = 0; i < steps; i++) {
            float f = 1f - (float) i / steps;
            int c = Colors.alpha(rgb, strength * f * f * 0.5f);
            int x0 = maxX * i / steps, x1 = maxX * (i + 1) / steps;
            int y0 = maxY * i / steps, y1 = maxY * (i + 1) / steps;
            if (x1 > x0) {
                g.fill(x0, 0, x1, h, c);
                g.fill(w - x1, 0, w - x0, h, c);
            }
            if (y1 > y0) {
                g.fill(0, y0, w, y1, c);
                g.fill(0, h - y1, w, h - y0, c);
            }
        }
    }
}
