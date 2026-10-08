package dev.lumina.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting {
    private boolean value;
    private final boolean def;

    public BoolSetting(Category c, String group, String id, String name, String desc, boolean def) {
        super(c, group, id, name, desc);
        this.value = def;
        this.def = def;
    }

    public boolean get() { return value; }

    public void set(boolean v) {
        if (v != value) {
            value = v;
            changed();
        }
    }

    /** Sets the value without persisting or running callbacks (used to mirror vanilla options). */
    public void setSilent(boolean v) { value = v; }

    public void toggle() { set(!value); }

    @Override public JsonElement save() { return new JsonPrimitive(value); }

    @Override public void load(JsonElement e) {
        try { value = e.getAsBoolean(); } catch (RuntimeException ignored) { }
    }

    @Override public void reset() { set(def); }

    @Override public boolean isDefault() { return value == def; }
}
