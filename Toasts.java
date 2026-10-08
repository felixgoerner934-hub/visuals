package dev.lumina.gui;

import dev.lumina.anim.Motion;
import dev.lumina.setting.Settings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Small slide-in notifications. */
public final class Toasts {
    private record Toast(String text, long born) {}

    private static final List<Toast> LIST = new ArrayList<>();
    private static final long LIFE_MS = 2600;

    private Toasts() {}

    public static void push(String text) {
        if (!Settings.NOTIFICATIONS.get()) return;
        if (LIST.size() >= 4) LIST.remove(0);
        LIST.add(new Toast(text, System.currentTimeMillis()));
    }

    public static boolean active() { return !LIST.isEmpty(); }

    /** Draws the stack so that the newest toast sits at {@code bottom}, right-aligned to {@code right}. */
    public static void render(GuiGraphicsExtractor g, Font font, int right, int bottom) {
        long now = System.currentTimeMillis();
        LIST.removeIf(t -> now - t.born > LIFE_MS);
        int y = bottom;
        boolean motion = Motion.on();
        for (int i = LIST.size() - 1; i >= 0; i--) {
            Toast t = LIST.get(i);
            long age = now - t.born;
            float in = motion ? Motion.outCubic(age / 220f) : 1f;
            float out = motion ? 1f - Motion.inCubic((age - (LIFE_MS - 260)) / 260f) : 1f;
            float a = Math.min(in, out);
            int w = font.width(t.text) + 22;
            int h = 20;
            int x = right - w + Math.round((1f - in) * 16f);
            float prev = Gfx.ga;
            Gfx.ga = prev * a;
            Gfx.shadow(g, x, y - h, w, h, 4, 0.5f);
            Gfx.round(g, x, y - h, w, h, 4, 0xF0000000 | (UiTheme.bg & 0xFFFFFF));
            Gfx.round(g, x, y - h + 3, 2, h - 6, 1, UiTheme.accent);
            Gfx.text(g, font, t.text, x + 10, y - h + 6, UiTheme.text, false);
            Gfx.ga = prev;
            y -= h + 4;
        }
    }
}
