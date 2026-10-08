package dev.lumina.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central definition of every setting. To add a new option, declare it here and read it where needed;
 * the menu, config file, presets and themes pick it up automatically.
 */
public final class Settings {
    private static final List<Setting> ALL = new ArrayList<>();
    private static final Map<String, Setting> BY_ID = new HashMap<>();

    private static <T extends Setting> T reg(T s) {
        ALL.add(s);
        BY_ID.put(s.id, s);
        return s;
    }

    private static BoolSetting bool(Category c, String g, String id, String n, String d, boolean def) {
        return reg(new BoolSetting(c, g, id, n, d, def));
    }

    private static NumberSetting num(Category c, String g, String id, String n, String d,
                                     double def, double min, double max, double step, String suffix) {
        return reg(new NumberSetting(c, g, id, n, d, def, min, max, step, suffix));
    }

    private static EnumSetting en(Category c, String g, String id, String n, String d, int def, String... o) {
        return reg(new EnumSetting(c, g, id, n, d, def, o));
    }

    private static ColorSetting col(Category c, String g, String id, String n, String d, int def) {
        return reg(new ColorSetting(c, g, id, n, d, def));
    }

    // ------------------------------------------------------------------ General
    public static final BoolSetting MASTER = bool(Category.GENERAL, "Main", "general.enabled",
            "Enable Visuals", "Master switch for HUD, crosshair and screen effects.", true);
    public static final BoolSetting NOTIFICATIONS = bool(Category.GENERAL, "Main", "general.notifications",
            "Notifications", "Show small pop-ups when presets or themes change.", true);

    // ------------------------------------------------------------------ HUD
    public static final BoolSetting HUD_ENABLED = bool(Category.HUD, "Info Panel", "hud.enabled",
            "Info Panel", "Show the compact info panel.", true);
    public static final EnumSetting HUD_POSITION = en(Category.HUD, "Info Panel", "hud.position",
            "Position", "Screen corner of the panel.", 0, "Top Left", "Top Right", "Bottom Left", "Bottom Right");
    public static final BoolSetting HUD_FPS = bool(Category.HUD, "Info Panel", "hud.show_fps",
            "Show FPS", "Frames per second.", true);
    public static final BoolSetting HUD_COORDS = bool(Category.HUD, "Info Panel", "hud.show_coords",
            "Show Coordinates", "XYZ position.", true);
    public static final BoolSetting HUD_DIRECTION = bool(Category.HUD, "Info Panel", "hud.show_direction",
            "Show Direction", "Compass direction you are facing.", true);
    public static final BoolSetting HUD_SPEED = bool(Category.HUD, "Info Panel", "hud.show_speed",
            "Show Speed", "Horizontal speed in blocks per second.", false);
    public static final BoolSetting HUD_TIME = bool(Category.HUD, "Info Panel", "hud.show_time",
            "Show Clock", "Real-world time.", false);

    public static final NumberSetting HUD_SCALE = num(Category.HUD, "Layout", "hud.scale",
            "HUD Scale", "Size of the info panel.", 1.0, 0.5, 2.0, 0.05, "x");
    public static final NumberSetting HUD_OPACITY = num(Category.HUD, "Layout", "hud.opacity",
            "HUD Opacity", "Overall opacity of the panel.", 1.0, 0.2, 1.0, 0.05, "");
    public static final NumberSetting HUD_MARGIN = num(Category.HUD, "Layout", "hud.margin",
            "Edge Margin", "Distance to the screen edge.", 6, 0, 30, 1, " px");
    public static final NumberSetting HUD_PADDING = num(Category.HUD, "Layout", "hud.padding",
            "Padding", "Space inside the panel.", 4, 2, 12, 1, " px");
    public static final NumberSetting HUD_SPACING = num(Category.HUD, "Layout", "hud.spacing",
            "HUD Spacing", "Space between lines.", 2, 0, 8, 1, " px");

    public static final BoolSetting HUD_BG = bool(Category.HUD, "Style", "hud.background",
            "HUD Background", "Draw a translucent background.", true);
    public static final NumberSetting HUD_BG_OPACITY = num(Category.HUD, "Style", "hud.bg_opacity",
            "Background Opacity", "Opacity of the background.", 0.45, 0.0, 1.0, 0.05, "");
    public static final NumberSetting HUD_ROUND = num(Category.HUD, "Style", "hud.rounded",
            "Rounded Corners", "Corner radius of the panel.", 3, 0, 10, 1, " px");
    public static final BoolSetting HUD_ACCENT_BAR = bool(Category.HUD, "Style", "hud.accent_bar",
            "Accent Bar", "Thin coloured bar on the panel edge.", true);
    public static final BoolSetting HUD_SHADOW = bool(Category.HUD, "Style", "hud.text_shadow",
            "Text Shadow", "Draw shadows below the text.", true);

    public static final ColorSetting HUD_COLOR_TEXT = col(Category.HUD, "Colors", "hud.color_text",
            "Text Color", "Colour of values.", 0xFFFFFF);
    public static final ColorSetting HUD_COLOR_LABEL = col(Category.HUD, "Colors", "hud.color_label",
            "Label Color", "Colour of labels and the accent bar.", 0xF0507A);
    public static final ColorSetting HUD_COLOR_BG = col(Category.HUD, "Colors", "hud.color_bg",
            "Background Color", "Colour of the panel background.", 0x000000);

    // ------------------------------------------------------------------ Crosshair
    public static final BoolSetting CH_ENABLED = bool(Category.CROSSHAIR, "Shape", "crosshair.enabled",
            "Custom Crosshair", "Replaces the vanilla crosshair.", true);
    public static final EnumSetting CH_STYLE = en(Category.CROSSHAIR, "Shape", "crosshair.style",
            "Style", "Shape of the crosshair.", 0,
            "Cross", "Cross + Dot", "Dot", "Circle", "Square", "T-Shape", "X-Shape");
    public static final NumberSetting CH_SIZE = num(Category.CROSSHAIR, "Shape", "crosshair.size",
            "Size", "Length of the arms / radius of shapes.", 5, 1, 24, 1, " px");
    public static final NumberSetting CH_THICKNESS = num(Category.CROSSHAIR, "Shape", "crosshair.thickness",
            "Thickness", "Line thickness.", 1, 1, 6, 1, " px");
    public static final NumberSetting CH_GAP = num(Category.CROSSHAIR, "Shape", "crosshair.gap",
            "Gap", "Empty space around the centre.", 3, 0, 16, 1, " px");

    public static final ColorSetting CH_COLOR = col(Category.CROSSHAIR, "Color", "crosshair.color",
            "Color", "Crosshair colour.", 0xFFFFFF);
    public static final NumberSetting CH_OPACITY = num(Category.CROSSHAIR, "Color", "crosshair.opacity",
            "Transparency", "Opacity of the crosshair.", 1.0, 0.2, 1.0, 0.05, "");
    public static final BoolSetting CH_OUTLINE = bool(Category.CROSSHAIR, "Color", "crosshair.outline",
            "Outline", "Dark outline for better contrast.", true);
    public static final ColorSetting CH_OUTLINE_COLOR = col(Category.CROSSHAIR, "Color", "crosshair.outline_color",
            "Outline Color", "Colour of the outline.", 0x000000);
    public static final BoolSetting CH_RAINBOW = bool(Category.CROSSHAIR, "Color", "crosshair.rainbow",
            "Rainbow", "Cycle through all hues.", false);
    public static final NumberSetting CH_RAINBOW_SPEED = num(Category.CROSSHAIR, "Color", "crosshair.rainbow_speed",
            "Rainbow Speed", "Cycles per second.", 0.5, 0.1, 3.0, 0.1, "x");

    public static final BoolSetting CH_DYNAMIC = bool(Category.CROSSHAIR, "Dynamic", "crosshair.dynamic",
            "Dynamic Crosshair", "Spreads while moving, sprinting or jumping.", false);
    public static final NumberSetting CH_DYNAMIC_AMOUNT = num(Category.CROSSHAIR, "Dynamic", "crosshair.dynamic_amount",
            "Spread Amount", "Maximum extra gap.", 4, 1, 12, 1, " px");

    public static final BoolSetting HIT_ENABLED = bool(Category.CROSSHAIR, "Hitmarker", "crosshair.hit.enabled",
            "Hitmarker", "Show a marker when you attack an entity.", true);
    public static final NumberSetting HIT_SIZE = num(Category.CROSSHAIR, "Hitmarker", "crosshair.hit.size",
            "Marker Size", "Length of the marker lines.", 5, 2, 12, 1, " px");
    public static final ColorSetting HIT_COLOR = col(Category.CROSSHAIR, "Hitmarker", "crosshair.hit.color",
            "Marker Color", "Colour of the marker.", 0xFF4D4D);
    public static final NumberSetting HIT_DURATION = num(Category.CROSSHAIR, "Hitmarker", "crosshair.hit.duration",
            "Marker Duration", "How long the marker stays.", 260, 80, 800, 10, " ms");

    // ------------------------------------------------------------------ Effects
    public static final BoolSetting FX_TINT = bool(Category.EFFECTS, "Color Tint", "fx.tint",
            "Screen Tint", "Overlay the screen with a colour.", false);
    public static final ColorSetting FX_TINT_COLOR = col(Category.EFFECTS, "Color Tint", "fx.tint_color",
            "Tint Color", "Colour of the tint.", 0xFF9A5C);
    public static final NumberSetting FX_TINT_OPACITY = num(Category.EFFECTS, "Color Tint", "fx.tint_opacity",
            "Tint Strength", "Opacity of the tint.", 0.10, 0.0, 0.5, 0.01, "");

    public static final BoolSetting FX_VIGNETTE = bool(Category.EFFECTS, "Vignette", "fx.vignette",
            "Vignette", "Darken the screen edges.", false);
    public static final NumberSetting FX_VIGNETTE_STRENGTH = num(Category.EFFECTS, "Vignette", "fx.vignette_strength",
            "Vignette Strength", "How dark the edges get.", 0.45, 0.05, 1.0, 0.05, "");
    public static final ColorSetting FX_VIGNETTE_COLOR = col(Category.EFFECTS, "Vignette", "fx.vignette_color",
            "Vignette Color", "Colour of the vignette.", 0x000000);

    public static final BoolSetting FX_LETTERBOX = bool(Category.EFFECTS, "Cinematic", "fx.letterbox",
            "Cinematic Bars", "Black bars at the top and bottom.", false);
    public static final NumberSetting FX_LETTERBOX_SIZE = num(Category.EFFECTS, "Cinematic", "fx.letterbox_size",
            "Bar Size", "Height of each bar in percent of the screen.", 10, 2, 20, 1, " %");

    public static final BoolSetting FX_HURT = bool(Category.EFFECTS, "Damage", "fx.hurt",
            "Custom Hurt Effect", "Coloured edge flash when you take damage.", true);
    public static final ColorSetting FX_HURT_COLOR = col(Category.EFFECTS, "Damage", "fx.hurt_color",
            "Hurt Color", "Colour of the flash.", 0xD81E2B);
    public static final NumberSetting FX_HURT_INTENSITY = num(Category.EFFECTS, "Damage", "fx.hurt_intensity",
            "Hurt Intensity", "Strength of the flash.", 0.55, 0.1, 1.0, 0.05, "");
    public static final NumberSetting FX_HURT_DURATION = num(Category.EFFECTS, "Damage", "fx.hurt_duration",
            "Hurt Duration", "How long the flash lasts.", 380, 100, 1200, 20, " ms");

    // ------------------------------------------------------------------ Player (mirrors vanilla options)
    public static final BoolSetting VIEW_BOBBING = bool(Category.PLAYER, "Camera", "player.bobbing",
            "View Bobbing", "Camera sway while walking (vanilla option).", true);
    public static final NumberSetting DAMAGE_TILT = num(Category.PLAYER, "Camera", "player.damage_tilt",
            "Damage Tilt", "Camera tilt when hurt (vanilla option).", 1.0, 0.0, 1.0, 0.05, "");
    public static final NumberSetting FOV_EFFECTS = num(Category.PLAYER, "Camera", "player.fov_effects",
            "FOV Effects", "Field-of-view change when sprinting etc. (vanilla option).", 1.0, 0.0, 1.0, 0.05, "");
    public static final NumberSetting DISTORTION = num(Category.PLAYER, "Camera", "player.distortion",
            "Distortion Effects", "Nausea and portal wobble (vanilla option).", 1.0, 0.0, 1.0, 0.05, "");

    // ------------------------------------------------------------------ Animations
    public static final BoolSetting ANIM_ENABLED = bool(Category.ANIMATIONS, "Global", "anim.enabled",
            "Animations", "Master switch for all animations.", true);
    public static final NumberSetting ANIM_SPEED = num(Category.ANIMATIONS, "Global", "anim.speed",
            "Animation Speed", "0 disables animations, higher is faster.", 1.0, 0.0, 3.0, 0.1, "x");
    public static final EnumSetting ANIM_OPEN = en(Category.ANIMATIONS, "Menu", "anim.open_style",
            "Open Style", "How the menu appears.", 0, "Scale + Fade", "Slide + Fade", "Fade", "None");
    public static final BoolSetting ANIM_SCROLL = bool(Category.ANIMATIONS, "Menu", "anim.smooth_scroll",
            "Smooth Scrolling", "Ease the scroll position.", true);
    public static final BoolSetting ANIM_HOVER = bool(Category.ANIMATIONS, "Menu", "anim.hover",
            "Hover Animation", "Fade highlights on hover.", true);
    public static final BoolSetting ANIM_FLASH = bool(Category.ANIMATIONS, "Menu", "anim.flash",
            "Setting Change Flash", "Briefly highlight a row when it changes.", true);
    public static final BoolSetting ANIM_HUD = bool(Category.ANIMATIONS, "HUD", "anim.hud",
            "HUD Animations", "Smooth crosshair spread, hitmarker and effect fades.", true);

    // ------------------------------------------------------------------ GUI
    public static final NumberSetting GUI_SCALE = num(Category.GUI, "Layout", "gui.scale",
            "UI Scale", "Size of this menu.", 1.0, 0.75, 1.5, 0.05, "x");
    public static final NumberSetting GUI_OPACITY = num(Category.GUI, "Layout", "gui.opacity",
            "UI Opacity", "Opacity of the menu panel.", 0.95, 0.4, 1.0, 0.01, "");
    public static final NumberSetting GUI_BACKDROP = num(Category.GUI, "Layout", "gui.backdrop",
            "Background Dim", "How much the game is darkened behind the menu.", 0.40, 0.0, 0.8, 0.05, "");
    public static final NumberSetting GUI_RADIUS = num(Category.GUI, "Style", "gui.corner_radius",
            "Corner Radius", "Roundness of the menu.", 6, 0, 12, 1, " px");
    public static final NumberSetting GUI_SHADOW = num(Category.GUI, "Style", "gui.shadow",
            "Shadow Strength", "Drop shadow below the menu.", 0.6, 0.0, 1.0, 0.05, "");
    public static final BoolSetting GUI_GLOW = bool(Category.GUI, "Style", "gui.glow",
            "Accent Glow", "Soft accent glow and moon motif.", true);
    public static final BoolSetting GUI_DESC = bool(Category.GUI, "Style", "gui.descriptions",
            "Show Descriptions", "Show a description below each setting.", true);
    public static final BoolSetting GUI_COMPACT = bool(Category.GUI, "Style", "gui.compact",
            "Compact Rows", "Smaller rows (hides descriptions).", false);

    // ------------------------------------------------------------------ Colors (menu)
    public static final ColorSetting C_ACCENT = col(Category.COLORS, "Menu Colors", "colors.accent",
            "Accent Color", "Highlights, toggles and sliders.", 0xF0507A);
    public static final ColorSetting C_BG = col(Category.COLORS, "Menu Colors", "colors.background",
            "Background Color", "Main panel colour.", 0x0F0F14);
    public static final ColorSetting C_PANEL = col(Category.COLORS, "Menu Colors", "colors.panel",
            "Secondary Color", "Sidebar and input fields.", 0x181820);
    public static final ColorSetting C_TEXT = col(Category.COLORS, "Menu Colors", "colors.text",
            "Text Color", "Primary text.", 0xF2F2F6);
    public static final ColorSetting C_TEXT2 = col(Category.COLORS, "Menu Colors", "colors.text_secondary",
            "Secondary Text Color", "Descriptions and hints.", 0x8E8E9C);

    // ------------------------------------------------------------------ Performance
    public static final BoolSetting LITE = bool(Category.PERFORMANCE, "Performance", "perf.lite",
            "Lite Mode", "Turns off screen effects and rounded HUD for minimum cost.", false);
    public static final NumberSetting HUD_INTERVAL = num(Category.PERFORMANCE, "Performance", "perf.hud_interval",
            "Info Panel Refresh", "How often the panel text is rebuilt.", 250, 50, 1000, 50, " ms");
    public static final BoolSetting MENU_SHADOW = bool(Category.PERFORMANCE, "Performance", "perf.menu_shadow",
            "Menu Shadow", "Disable to save a few draw calls in the menu.", true);
    public static final BoolSetting HUD_ROUNDED = bool(Category.PERFORMANCE, "Performance", "perf.hud_rounded",
            "Rounded HUD Corners", "Disable to draw plain rectangles.", true);
    public static final EnumSetting CIRCLE_QUALITY = en(Category.PERFORMANCE, "Performance", "perf.circle_quality",
            "Circle Quality", "Segments used for the circle crosshair.", 1, "Low", "Medium", "High");

    // ------------------------------------------------------------------ Accessibility
    public static final BoolSetting REDUCE_MOTION = bool(Category.ACCESSIBILITY, "Accessibility", "access.reduce_motion",
            "Reduce Motion", "Disables every animation.", false);
    public static final BoolSetting HIGH_CONTRAST = bool(Category.ACCESSIBILITY, "Accessibility", "access.high_contrast",
            "High Contrast", "Brighter secondary text and forced crosshair outline.", false);
    public static final BoolSetting KEY_HINTS = bool(Category.ACCESSIBILITY, "Accessibility", "access.keyboard_hints",
            "Keyboard Hints", "Show keyboard shortcuts at the bottom of the menu.", true);

    static {
        // Mirror vanilla options: applied immediately when changed in the menu.
        VIEW_BOBBING.onChange(dev.lumina.client.VanillaBridge::push);
        DAMAGE_TILT.onChange(dev.lumina.client.VanillaBridge::push);
        FOV_EFFECTS.onChange(dev.lumina.client.VanillaBridge::push);
        DISTORTION.onChange(dev.lumina.client.VanillaBridge::push);
    }

    private Settings() {}

    public static List<Setting> all() { return Collections.unmodifiableList(ALL); }

    public static Setting byId(String id) { return BY_ID.get(id); }

    /** Settings that make up a preset (everything visual that is not menu styling or system state). */
    public static boolean isPresetScoped(Setting s) {
        return s.category == Category.HUD || s.category == Category.CROSSHAIR || s.category == Category.EFFECTS;
    }

    /** Settings that make up a theme. */
    public static final String[] THEME_IDS = {
            "colors.accent", "colors.background", "colors.panel", "colors.text", "colors.text_secondary",
            "gui.opacity", "gui.corner_radius", "gui.shadow"
    };

    public static boolean isThemeScoped(Setting s) {
        for (String id : THEME_IDS) if (id.equals(s.id)) return true;
        return false;
    }
}
