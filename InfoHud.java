package dev.lumina.hud;

import dev.lumina.gui.Colors;
import dev.lumina.gui.Gfx;
import dev.lumina.setting.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The compact info panel. Text is rebuilt only every few hundred milliseconds. */
public final class InfoHud {
    private static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private static final List<String[]> LINES = new ArrayList<>();
    private static long lastBuild = 0L;
    private static int labelW;
    private static int valueW;

    // FPS measurement
    private static int frames;
    private static long fpsWindowStart = System.nanoTime();
    private static int fps;

    private InfoHud() {}

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        frames++;
        long now = System.nanoTime();
        if (now - fpsWindowStart >= 1_000_000_000L) {
            fps = Math.round(frames * 1e9f / (now - fpsWindowStart));
            frames = 0;
            fpsWindowStart = now;
        }

        if (!Settings.MASTER.get() || !Settings.HUD_ENABLED.get()) return;
        if (mc.screen != null && !(mc.screen instanceof dev.lumina.gui.LuminaScreen)) return;
        Font font = mc.font;

        long ms = now / 1_000_000L;
        if (ms - lastBuild >= Settings.HUD_INTERVAL.getI()) {
            lastBuild = ms;
            rebuild(mc.player, font);
        }
        if (LINES.isEmpty()) return;

        int pad = Settings.HUD_PADDING.getI();
        int spacing = Settings.HUD_SPACING.getI();
        int lh = font.lineHeight + spacing;
        int gapMid = 6;
        int w = labelW + gapMid + valueW + pad * 2 + (Settings.HUD_ACCENT_BAR.get() ? 3 : 0);
        int h = LINES.size() * lh - spacing + pad * 2;
        float scale = Settings.HUD_SCALE.getF();
        int margin = Settings.HUD_MARGIN.getI();
        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();

        int pos = Settings.HUD_POSITION.index();
        float x = (pos == 1 || pos == 3) ? sw - margin - w * scale : margin;
        float y = (pos >= 2) ? sh - margin - h * scale : margin;

        Gfx.ga = Settings.HUD_OPACITY.getF();
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);

        int radius = (Settings.HUD_ROUNDED.get() && !Settings.LITE.get()) ? Settings.HUD_ROUND.getI() : 0;
        if (Settings.HUD_BG.get()) {
            Gfx.round(g, 0, 0, w, h, radius, Colors.alpha(Settings.HUD_COLOR_BG.get(), Settings.HUD_BG_OPACITY.getF()));
        }
        int labelColor = 0xFF000000 | Settings.HUD_COLOR_LABEL.get();
        int textColor = 0xFF000000 | Settings.HUD_COLOR_TEXT.get();
        int tx = pad;
        if (Settings.HUD_ACCENT_BAR.get()) {
            Gfx.round(g, 0, Math.min(2, h / 4), 2, h - Math.min(2, h / 4) * 2, 1, labelColor);
            tx += 3;
        }
        boolean shadow = Settings.HUD_SHADOW.get();
        int ty = pad;
        for (String[] line : LINES) {
            Gfx.text(g, font, line[0], tx, ty, labelColor, shadow);
            Gfx.text(g, font, line[1], tx + labelW + gapMid, ty, textColor, shadow);
            ty += lh;
        }
        g.pose().popMatrix();
        Gfx.ga = 1f;
    }

    private static void rebuild(LocalPlayer p, Font font) {
        LINES.clear();
        if (Settings.HUD_FPS.get()) LINES.add(new String[]{"FPS", Integer.toString(fps)});
        if (Settings.HUD_COORDS.get()) {
            LINES.add(new String[]{"XYZ", String.format(java.util.Locale.ROOT, "%.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ())});
        }
        if (Settings.HUD_DIRECTION.get()) {
            float yaw = Mth.wrapDegrees(p.getYRot());
            int idx = ((int) Math.floor((yaw + 180f) / 45f + 0.5f)) & 7;
            LINES.add(new String[]{"DIR", DIRS[idx]});
        }
        if (Settings.HUD_SPEED.get()) {
            double bps = p.getDeltaMovement().horizontalDistance() * 20.0;
            LINES.add(new String[]{"SPD", String.format(java.util.Locale.ROOT, "%.1f b/s", bps)});
        }
        if (Settings.HUD_TIME.get()) LINES.add(new String[]{"TIME", LocalTime.now().format(CLOCK)});
        labelW = 0;
        valueW = 0;
        for (String[] l : LINES) {
            labelW = Math.max(labelW, font.width(l[0]));
            valueW = Math.max(valueW, font.width(l[1]));
        }
    }
}
