package dev.lumina.setting;

/** Pages of the settings menu. {@code library} pages list presets/themes instead of settings. */
public enum Category {
    GENERAL("General", false),
    HUD("HUD", false),
    CROSSHAIR("Crosshair", false),
    EFFECTS("Effects", false),
    PLAYER("Player", false),
    ANIMATIONS("Animations", false),
    GUI("GUI", false),
    COLORS("Colors", false),
    PERFORMANCE("Performance", false),
    ACCESSIBILITY("Accessibility", false),
    PRESETS("Presets", true),
    THEMES("Themes", true);

    public final String label;
    public final boolean library;

    Category(String label, boolean library) {
        this.label = label;
        this.library = library;
    }
}
