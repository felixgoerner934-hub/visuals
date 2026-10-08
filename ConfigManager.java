package dev.lumina.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumina.LuminaMod;
import dev.lumina.setting.Setting;
import dev.lumina.setting.Settings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Loads and saves {@code config/lumina/config.json} plus user presets/themes. Writes are atomic and batched. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile boolean dirty;
    private static volatile long lastChangeMs;

    private ConfigManager() {}

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("lumina");
    }

    public static void markDirty() {
        dirty = true;
        lastChangeMs = System.currentTimeMillis();
    }

    public static void load() {
        Path file = dir().resolve("config.json");
        if (!Files.exists(file)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject values = root.getAsJsonObject("settings");
            if (values != null) {
                for (Setting s : Settings.all()) {
                    JsonElement e = values.get(s.id);
                    if (e != null) s.load(e);
                }
            }
        } catch (Exception ex) {
            LuminaMod.LOGGER.warn("Could not read config, using defaults: {}", ex.toString());
        }
        dirty = false;
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("settings", snapshot(s -> true));
        try {
            writeAtomic(dir().resolve("config.json"), GSON.toJson(root));
            dirty = false;
        } catch (IOException ex) {
            LuminaMod.LOGGER.error("Could not save config", ex);
        }
    }

    public static void saveIfDirty() {
        if (dirty) save();
    }

    /** Saves a moment after the last change so dragging a slider does not hit the disk every frame. */
    public static void saveDebounced() {
        if (dirty && System.currentTimeMillis() - lastChangeMs > 800) save();
    }

    public static JsonObject snapshot(Predicate<Setting> filter) {
        JsonObject o = new JsonObject();
        for (Setting s : Settings.all()) {
            if (filter.test(s)) o.add(s.id, s.save());
        }
        return o;
    }

    // ---------------------------------------------------------------- user libraries (presets / themes)

    public static String sanitize(String name) {
        String n = name.replaceAll("[^A-Za-z0-9 _\\-]", "").trim();
        return n.length() > 24 ? n.substring(0, 24).trim() : n;
    }

    private static Path libDir(String kind) {
        return dir().resolve(kind);
    }

    public static List<String> listLibrary(String kind) {
        List<String> names = new ArrayList<>();
        Path d = libDir(kind);
        if (!Files.isDirectory(d)) return names;
        try (Stream<Path> files = Files.list(d)) {
            files.map(p -> p.getFileName().toString())
                    .filter(f -> f.endsWith(".json"))
                    .map(f -> f.substring(0, f.length() - 5))
                    .sorted(Comparator.comparing(String::toLowerCase))
                    .forEach(names::add);
        } catch (IOException ex) {
            LuminaMod.LOGGER.warn("Could not list {}: {}", kind, ex.toString());
        }
        return names;
    }

    public static JsonObject readLibrary(String kind, String name) {
        Path f = libDir(kind).resolve(name + ".json");
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            return root.getAsJsonObject("settings");
        } catch (Exception ex) {
            LuminaMod.LOGGER.warn("Could not read {} '{}': {}", kind, name, ex.toString());
            return null;
        }
    }

    public static boolean writeLibrary(String kind, String name, JsonObject settings) {
        JsonObject root = new JsonObject();
        root.addProperty("name", name);
        root.add("settings", settings);
        try {
            writeAtomic(libDir(kind).resolve(name + ".json"), GSON.toJson(root));
            return true;
        } catch (IOException ex) {
            LuminaMod.LOGGER.error("Could not write {} '{}'", kind, name, ex);
            return false;
        }
    }

    public static void deleteLibrary(String kind, String name) {
        try {
            Files.deleteIfExists(libDir(kind).resolve(name + ".json"));
        } catch (IOException ex) {
            LuminaMod.LOGGER.warn("Could not delete {} '{}': {}", kind, name, ex.toString());
        }
    }

    private static void writeAtomic(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(content);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailed) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
