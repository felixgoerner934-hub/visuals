package dev.lumina.config;

import java.util.LinkedHashMap;
import java.util.Map;

/** Hard-coded presets and themes. Values not listed fall back to the setting default. */
final class BuiltIns {
    static final Map<String, Map<String, Object>> PRESETS = new LinkedHashMap<>();
    static final Map<String, Map<String, Object>> THEMES = new LinkedHashMap<>();

    private BuiltIns() {}

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    static {
        PRESETS.put("Minimal", map(
                "hud.show_coords", false, "hud.show_direction", false, "hud.background", false,
                "hud.accent_bar", false, "crosshair.style", "Dot", "crosshair.outline", false,
                "crosshair.hit.enabled", false, "fx.hurt", false));
        PRESETS.put("Clean", map());
        PRESETS.put("Cinematic", map(
                "hud.enabled", false, "crosshair.style", "Dot", "crosshair.opacity", 0.6,
                "crosshair.outline", false, "crosshair.hit.enabled", false,
                "fx.vignette", true, "fx.vignette_strength", 0.6,
                "fx.letterbox", true, "fx.letterbox_size", 11,
                "fx.tint", true, "fx.tint_color", "#FFB070", "fx.tint_opacity", 0.07,
                "fx.hurt_intensity", 0.4));
        PRESETS.put("Competitive", map(
                "hud.show_speed", true, "hud.scale", 0.85, "hud.bg_opacity", 0.3,
                "crosshair.style", "Cross", "crosshair.gap", 2, "crosshair.size", 6,
                "crosshair.color", "#4DFF88", "crosshair.outline", true,
                "crosshair.hit.enabled", true, "crosshair.hit.duration", 200,
                "fx.hurt", true, "fx.hurt_intensity", 0.35, "fx.hurt_duration", 250));
        PRESETS.put("Vanilla+", map(
                "hud.enabled", false, "crosshair.style", "Cross", "crosshair.size", 5,
                "crosshair.gap", 0, "crosshair.thickness", 1, "crosshair.outline", false,
                "crosshair.opacity", 0.9, "crosshair.hit.enabled", false, "fx.hurt", false));
        PRESETS.put("Ultra", map(
                "hud.show_speed", true, "hud.show_time", true, "hud.bg_opacity", 0.55, "hud.rounded", 6,
                "crosshair.style", "Circle", "crosshair.size", 7, "crosshair.dynamic", true,
                "crosshair.color", "#F0507A", "crosshair.hit.size", 7,
                "fx.vignette", true, "fx.vignette_strength", 0.4,
                "fx.tint", true, "fx.tint_color", "#F0507A", "fx.tint_opacity", 0.05,
                "fx.hurt_intensity", 0.7));

        THEMES.put("Dark", map(
                "colors.accent", "#F0507A", "colors.background", "#0F0F14", "colors.panel", "#181820",
                "colors.text", "#F2F2F6", "colors.text_secondary", "#8E8E9C",
                "gui.opacity", 0.95, "gui.corner_radius", 6, "gui.shadow", 0.6));
        THEMES.put("Midnight", map(
                "colors.accent", "#7C8CFF", "colors.background", "#0A0D1A", "colors.panel", "#121830",
                "colors.text", "#E8ECFF", "colors.text_secondary", "#7E88B5",
                "gui.opacity", 0.95, "gui.corner_radius", 7, "gui.shadow", 0.7));
        THEMES.put("Minimal", map(
                "colors.accent", "#FFFFFF", "colors.background", "#161616", "colors.panel", "#202020",
                "colors.text", "#EDEDED", "colors.text_secondary", "#8A8A8A",
                "gui.opacity", 1.0, "gui.corner_radius", 2, "gui.shadow", 0.0));
        THEMES.put("Glass", map(
                "colors.accent", "#6FE3FF", "colors.background", "#101826", "colors.panel", "#2A3850",
                "colors.text", "#FFFFFF", "colors.text_secondary", "#A9B8D0",
                "gui.opacity", 0.62, "gui.corner_radius", 10, "gui.shadow", 0.5));
        THEMES.put("AMOLED", map(
                "colors.accent", "#FF2D6F", "colors.background", "#000000", "colors.panel", "#0B0B0D",
                "colors.text", "#FFFFFF", "colors.text_secondary", "#7A7A82",
                "gui.opacity", 1.0, "gui.corner_radius", 4, "gui.shadow", 0.0));
        THEMES.put("Light", map(
                "colors.accent", "#E0245E", "colors.background", "#F4F4F8", "colors.panel", "#FFFFFF",
                "colors.text", "#17171C", "colors.text_secondary", "#6A6A76",
                "gui.opacity", 0.98, "gui.corner_radius", 6, "gui.shadow", 0.35));
    }
}
