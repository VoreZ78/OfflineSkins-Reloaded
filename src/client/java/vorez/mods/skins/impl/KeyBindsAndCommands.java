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
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class  KeyBindsAndCommands {

    public final static class ModKeyBindings {
        private static boolean openConfigNextTick = false;

        public static KeyMapping OPEN_CONFIG;

        private ModKeyBindings() {
        }

        private static final KeyMapping.Category OfflineSkinsReloadedCategory =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("offlineskins-reloaded", "main")
        );

        public static void register() {
            OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key.offlineskins-reloaded",
                            InputConstants.Type.KEYSYM,
                            GLFW.GLFW_KEY_U,
                            OfflineSkinsReloadedCategory
                    )
            );

            ClientTickEvents.END_CLIENT_TICK.register(client -> tick());

            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                    ClientCommands.literal("offlineskins")
                            .then(ClientCommands.literal("menu")
                                    .executes(context -> {
                                        openConfigNextTick = true;
                                        return 1;
                                    })
                            )
                            .then(ClientCommands.literal("version")
                                    .executes(context -> {
                                        String version = FabricLoader.getInstance()
                                                .getModContainer("offlineskins-reloaded")
                                                .map(container -> container.getMetadata()
                                                        .getVersion()
                                                        .getFriendlyString())
                                                .orElse("Unknown");

                                        Minecraft client = Minecraft.getInstance();

                                        if (client.player != null) {
                                            MutableComponent message = Component.empty()
                                                    .append(Component.literal("OfflineSkins Reloaded ")
                                                            .withStyle(ChatFormatting.GREEN))
                                                    .append(Component.translatable(
                                                            "text.offlineskins-reloaded.version_text"
                                                    ).withStyle(ChatFormatting.GRAY))
                                                    .append(Component.literal(version)
                                                            .withStyle(ChatFormatting.GREEN));

                                            client.player.sendOverlayMessage(message);
                                        }

                                        return 1;
                                    })
                            )

            ));
        }

        public static void tick() {
            Minecraft client = Minecraft.getInstance();

            while (OPEN_CONFIG.consumeClick()) {
                Screen current = client.screen;

                client.setScreen(
                        YaclSettings.createConfigScreen(current)
                );
            }

            if (openConfigNextTick) {
                openConfigNextTick = false;

                client.setScreen(
                        YaclSettings.createConfigScreen(null)
                );
            }
        }
    }
}