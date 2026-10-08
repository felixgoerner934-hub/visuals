package dev.lumina.setting;

import com.google.gson.JsonElement;
import dev.lumina.config.ConfigManager;

/** Base class of every configurable value. The menu is generated from these objects. */
public abstract class Setting {
    public final Category category;
    public final String group;
    public final String id;
    public final String name;
    public final String desc;
    private Runnable onChange;

    protected Setting(Category category, String group, String id, String name, String desc) {
        this.category = category;
        this.group = group;
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    /** Registers a callback that runs whenever the value is changed through {@code set}. */
    public Setting onChange(Runnable r) {
        this.onChange = r;
        return this;
    }

    protected void changed() {
        ConfigManager.markDirty();
        if (onChange != null) onChange.run();
    }

    public abstract JsonElement save();

    /** Loads a value without triggering callbacks. Invalid input is ignored. */
    public abstract void load(JsonElement element);

    public abstract void reset();

    public abstract boolean isDefault();
}
