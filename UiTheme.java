package dev.lumina.gui;

import dev.lumina.setting.Settings;

/** Colours derived from the user's GUI settings. Call {@link #refresh()} once per frame before drawing. */
public final class UiTheme {
    public static int accent;
    public static int accentSoft;
    public static int bg;
    public static int panel;
    public static int text;
    public static int textDim;
    public static int border;
    public static int hover;
    public static int track;
    public static int radius;
    public static float shadow;
    public static float panelOpacity;

    private UiTheme() {}

    public static void refresh() {
        int a = Settings.C_ACCENT.get();
        int t = Settings.C_TEXT.get();
        panelOpacity = Settings.GUI_OPACITY.getF();
        accent = 0xFF000000 | a;
        accentSoft = Colors.alpha(a, 0.18f);
        bg = Colors.alpha(Settings.C_BG.get(), panelOpacity);
        panel = Colors.alpha(Settings.C_PANEL.get(), Math.min(1f, panelOpacity + 0.05f));
        text = 0xFF000000 | t;
        textDim = Settings.HIGH_CONTRAST.get() ? text : (0xFF000000 | Settings.C_TEXT2.get());
        border = Colors.alpha(t, Settings.HIGH_CONTRAST.get() ? 0.35f : 0.10f);
        hover = Colors.alpha(t, 0.07f);
        track = Colors.alpha(t, 0.16f);
        radius = Settings.GUI_RADIUS.getI();
        shadow = Settings.GUI_SHADOW.getF();
    }
}
