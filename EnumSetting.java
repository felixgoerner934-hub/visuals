package dev.lumina.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A setting that picks one of several named options. */
public final class EnumSetting extends Setting {
    public final String[] options;
    private int index;
    private final int def;

    public EnumSetting(Category c, String group, String id, String name, String desc, int def, String... options) {
        super(c, group, id, name, desc);
        this.options = options;
        this.def = def;
        this.index = def;
    }

    public int index() { return index; }
    public String get() { return options[index]; }

    public void setIndex(int i) {
        i = Math.floorMod(i, options.length);
        if (i != index) {
            index = i;
            changed();
        }
    }

    public void cycle(int dir) { setIndex(index + dir); }

    @Override public JsonElement save() { return new JsonPrimitive(options[index]); }

    @Override public void load(JsonElement e) {
        try {
            String s = e.getAsString();
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(s)) { index = i; return; }
            }
        } catch (RuntimeException ignored) { }
    }

    @Override public void reset() { setIndex(def); }

    @Override public boolean isDefault() { return index == def; }
}
