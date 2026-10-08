package dev.lumina.client;

import dev.lumina.setting.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/** Mirrors a few vanilla options so they can be edited from the Lumina menu. Kept separate so it is easy to adjust. */
public final class VanillaBridge {
    private VanillaBridge() {}

    /** Writes the Lumina values into the vanilla options. */
    public static void push() {
        Options o = Minecraft.getInstance().options;
        if (o == null) return;
        o.bobView().set(Settings.VIEW_BOBBING.get());
        o.damageTiltStrength().set(Settings.DAMAGE_TILT.get());
        o.fovEffectScale().set(Settings.FOV_EFFECTS.get());
        o.screenEffectScale().set(Settings.DISTORTION.get());
    }

    /** Reads the vanilla values into the Lumina settings (without saving), called when the menu opens. */
    public static void pull() {
        Options o = Minecraft.getInstance().options;
        if (o == null) return;
        Settings.VIEW_BOBBING.setSilent(o.bobView().get());
        Settings.DAMAGE_TILT.setSilent(o.damageTiltStrength().get());
        Settings.FOV_EFFECTS.setSilent(o.fovEffectScale().get());
        Settings.DISTORTION.setSilent(o.screenEffectScale().get());
    }

    public static void saveVanilla() {
        Options o = Minecraft.getInstance().options;
        if (o != null) o.save();
    }
}
