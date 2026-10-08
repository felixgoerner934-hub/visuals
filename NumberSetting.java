package dev.lumina.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting {
    private double value;
    private final double def;
    public final double min;
    public final double max;
    public final double step;
    public final String suffix;

    public NumberSetting(Category c, String group, String id, String name, String desc,
                         double def, double min, double max, double step, String suffix) {
        super(c, group, id, name, desc);
        this.def = def;
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.value = def;
    }

    public double get() { return value; }
    public float getF() { return (float) value; }
    public int getI() { return (int) Math.round(value); }

    private double normalize(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) v = Math.round((v - min) / step) * step + min;
        v = Math.round(v * 10000.0) / 10000.0;
        return Math.max(min, Math.min(max, v));
    }

    public void set(double v) {
        v = normalize(v);
        if (v != value) {
            value = v;
            changed();
        }
    }

    public void setSilent(double v) { value = normalize(v); }

    /** Position of the value between min and max (0..1). */
    public float fraction() { return (float) ((value - min) / (max - min)); }

    public String display() {
        String num = step >= 1 ? Long.toString(Math.round(value))
                : (step >= 0.1 ? String.format(java.util.Locale.ROOT, "%.1f", value)
                : String.format(java.util.Locale.ROOT, "%.2f", value));
        return num + suffix;
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }

    @Override public void load(JsonElement e) {
        try { value = normalize(e.getAsDouble()); } catch (RuntimeException ignored) { }
    }

    @Override public void reset() { set(def); }

    @Override public boolean isDefault() { return value == def; }
}
