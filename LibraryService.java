package dev.lumina.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lumina.setting.Setting;
import dev.lumina.setting.Settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Built-in and user-saved presets and themes. */
public final class LibraryService {
    private static final Gson GSON = new Gson();

    public enum Kind {
        PRESETS("presets", Settings::isPresetScoped),
        THEMES("themes", Settings::isThemeScoped);

        public final String folder;
        public final Predicate<Setting> scope;

        Kind(String folder, Predicate<Setting> scope) {
            this.folder = folder;
            this.scope = scope;
        }

        Map<String, Map<String, Object>> builtIns() {
            return this == PRESETS ? BuiltIns.PRESETS : BuiltIns.THEMES;
        }
    }

    public record Entry(String name, boolean builtIn) {}

    private LibraryService() {}

    public static List<Entry> list(Kind kind) {
        List<Entry> out = new ArrayList<>();
        for (String n : kind.builtIns().keySet()) out.add(new Entry(n, true));
        for (String n : ConfigManager.listLibrary(kind.folder)) out.add(new Entry(n, false));
        return out;
    }

    /** Resets every setting in the kind's scope, then applies the stored overrides. */
    public static boolean apply(Kind kind, Entry entry) {
        Map<String, JsonElement> values = new LinkedHashMap<>();
        if (entry.builtIn()) {
            Map<String, Object> raw = kind.builtIns().get(entry.name());
            if (raw == null) return false;
            raw.forEach((k, v) -> values.put(k, GSON.toJsonTree(v)));
        } else {
            JsonObject o = ConfigManager.readLibrary(kind.folder, entry.name());
            if (o == null) return false;
            o.entrySet().forEach(e -> values.put(e.getKey(), e.getValue()));
        }
        for (Setting s : Settings.all()) {
            if (kind.scope.test(s)) s.reset();
        }
        values.forEach((id, el) -> {
            Setting s = Settings.byId(id);
            if (s != null && kind.scope.test(s)) s.load(el);
        });
        ConfigManager.markDirty();
        return true;
    }

    /** Saves the current values as a user preset/theme. Existing entries with the same name are overwritten. */
    public static boolean saveCurrent(Kind kind, String rawName) {
        String name = ConfigManager.sanitize(rawName);
        if (name.isEmpty()) return false;
        for (String builtIn : kind.builtIns().keySet()) {
            if (builtIn.equalsIgnoreCase(name)) return false;
        }
        return ConfigManager.writeLibrary(kind.folder, name, ConfigManager.snapshot(kind.scope));
    }

    public static void delete(Kind kind, String name) {
        ConfigManager.deleteLibrary(kind.folder, name);
    }
}
