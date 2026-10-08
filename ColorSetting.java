package dev.lumina.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An RGB colour stored as {@code 0xRRGGBB}. */
public final class ColorSetting extends Setting {
    private int value;
    private final int def;

    public ColorSetting(Category c, String group, String id, String name, String desc, int def) {
        super(c, group, id, name, desc);
        this.def = def & 0xFFFFFF;
        this.value = this.def;
    }

    public int get() { return value; }

    public void set(int rgb) {
        rgb &= 0xFFFFFF;
        if (rgb != value) {
            value = rgb;
            changed();
        }
    }

    public String hex() { return String.format("#%06X", value); }

    @Override public JsonElement save() { return new JsonPrimitive(hex()); }

    @Override public void load(JsonElement e) {
        try {
            String s = e.getAsString().trim();
            if (s.startsWith("#")) s = s.substring(1);
            value = Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (RuntimeException ignored) { }
    }

    @Override public void reset() { set(def); }

    @Override public boolean isDefault() { return value == def; }
}
