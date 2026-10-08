package dev.lumina.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumina.LuminaMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The menu keybind. It is a normal Minecraft key mapping, so it can be changed in
 * Options > Controls > Key Binds > "Lumina Visuals" > "Open Visual Settings".
 * <p>
 * Default: the key that is labelled "Ü" on a German keyboard. GLFW names keys after the US layout,
 * where that physical key is the left square bracket.
 */
public final class LuminaKeys {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(LuminaMod.MOD_ID, "main"));

    public static final KeyMapping OPEN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.lumina.open_settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_BRACKET,
            CATEGORY));

    private LuminaKeys() {}

    /** Forces class loading so the key is registered during client start-up. */
    public static void init() {}
}
