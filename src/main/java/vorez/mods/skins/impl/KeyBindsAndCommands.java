package vorez.mods.skins.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public class KeyBindsAndCommands {

    public static final class ModKeyBindings {

        private static boolean openConfigNextTick = false;

        public static final KeyMapping OPEN_CONFIG = new KeyMapping(
                "key.offlineskins-reloaded",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "OfflineSkins Reloaded"
        );

        private ModKeyBindings() {
        }

        public static void register(IEventBus modEventBus) {
            modEventBus.addListener(ModKeyBindings::registerKeyMappings);
            NeoForge.EVENT_BUS.addListener(ModKeyBindings::registerClientCommands);
            NeoForge.EVENT_BUS.addListener(ModKeyBindings::onClientTick);
        }

        private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_CONFIG);
        }

        private static void registerClientCommands(RegisterClientCommandsEvent event)
        {
            event.getDispatcher().register(
                    Commands.literal("offlineskins")
                            .then(Commands.literal("menu")
                                    .executes(context -> {
                                        openConfigNextTick = true;
                                        return 1;
                                    })
                            )
                            .then(Commands.literal("version")
                                    .executes(context -> {
                                        String version = ModList.get()
                                                .getModContainerById("offlineskins_reloaded")
                                                .map(container -> container
                                                        .getModInfo()
                                                        .getVersion()
                                                        .toString())
                                                .orElse("Unknown");

                                        Minecraft client = Minecraft.getInstance();

                                        if (client.player != null) {
                                            MutableComponent message =
                                                    Component.empty()
                                                            .append(
                                                                    Component.literal("OfflineSkins Reloaded ")
                                                                            .withStyle(
                                                                                    ChatFormatting.GREEN
                                                                    )
                                                            )
                                                            .append(
                                                                    Component.translatable("text.offlineskins-reloaded.version_text")
                                                                            .withStyle(
                                                                                    ChatFormatting.GRAY
                                                                    )
                                                            )
                                                            .append(
                                                                    Component.literal(version)
                                                                            .withStyle(
                                                                                    ChatFormatting.GREEN
                                                                    )
                                                            );

                                            client.player.displayClientMessage(message, true);
                                        }

                                        return 1;
                                    })
                            )
            );
        }

        private static void onClientTick(ClientTickEvent.Post event) {
            tick();
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