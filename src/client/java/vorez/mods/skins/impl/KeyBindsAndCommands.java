package vorez.mods.skins.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public class KeyBindsAndCommands {

    public final static class ModKeyBindings {

        public static KeyMapping OPEN_CONFIG;

        private static final KeyMapping.Category CATEGORY =
                KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("offlineskins-reloaded", "main"));

        private ModKeyBindings() {
        }

        public static void register() {
            OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key.offlineskins-reloaded",
                            InputConstants.Type.KEYSYM,
                            GLFW.GLFW_KEY_U,
                            CATEGORY
                    )
            );

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                tick();
            });

            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                dispatcher.register(ClientCommands.literal("offlineskins")
                        .then(ClientCommands.literal("menu")
                                .executes(context -> {
                                    Minecraft client = Minecraft.getInstance();
                                    client.execute(() -> {
                                        client.setScreen(YaclSettings.createConfigScreen(client.screen));
                                    });
                                    return 1;
                                })
                        )
                        .then(ClientCommands.literal("version")
                                .executes(context -> {
                                    String version = FabricLoader.getInstance()
                                            .getModContainer("offlineskins-reloaded")
                                            .map(container -> container.getMetadata().getVersion().getFriendlyString())
                                            .orElse("Unknown");

                                    Minecraft client = Minecraft.getInstance();
                                    if (client.player != null) {
                                        MutableComponent message = Component.empty()
                                                .append(Component.literal("OfflineSkins Reloaded ")
                                                        .withStyle(ChatFormatting.GREEN))
                                                .append(Component.translatable("text.offlineskins-reloaded.version_text")
                                                        .withStyle(ChatFormatting.GRAY))
                                                .append(Component.translatable(version).withStyle((ChatFormatting.GOLD)));

                                        client.player.sendOverlayMessage(message);
                                    }
                                    return 1;
                                })
                        )
                );
            });
        }

        public static void tick() {
            Minecraft client = Minecraft.getInstance();

            while (OPEN_CONFIG.consumeClick()) {
                Screen current = client.screen;

                client.setScreen(
                        YaclSettings.createConfigScreen(current)
                );
            }
        }
    }
}
