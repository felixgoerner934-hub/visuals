package dev.lumina.client;

import dev.lumina.LuminaMod;
import dev.lumina.config.ConfigManager;
import dev.lumina.gui.LuminaScreen;
import dev.lumina.gui.Toasts;
import dev.lumina.gui.UiTheme;
import dev.lumina.hud.Crosshair;
import dev.lumina.hud.Effects;
import dev.lumina.hud.InfoHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

/** Client entry point: config, keybind, HUD layer and attack hook. */
public final class LuminaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        LuminaKeys.init();

        // The only per-tick work: checking whether the menu key was pressed.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (LuminaKeys.OPEN.consumeClick()) {
                if (client.screen == null && client.player != null) {
                    client.setScreen(new LuminaScreen());
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.saveIfDirty());

        // Hitmarker trigger: remember when the local player attacks something.
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (player == Minecraft.getInstance().player) Crosshair.markHit();
            return InteractionResult.PASS;
        });

        // The vanilla crosshair is replaced by our own renderer (which also offers a vanilla-like style).
        HudElementRegistry.removeElement(VanillaHudElements.CROSSHAIR);
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(LuminaMod.MOD_ID, "overlay"),
                (graphics, delta) -> renderOverlay(graphics));

        LuminaMod.LOGGER.info("{} loaded", LuminaMod.NAME);
    }

    private static void renderOverlay(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        Effects.render(g);
        Crosshair.render(g);
        InfoHud.render(g);
        if (Toasts.active() && !(mc.screen instanceof LuminaScreen)) {
            UiTheme.refresh();
            Toasts.render(g, mc.font, mc.getWindow().getGuiScaledWidth() - 10, mc.getWindow().getGuiScaledHeight() - 36);
        }
    }
}
