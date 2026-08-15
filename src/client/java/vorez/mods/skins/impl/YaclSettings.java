package vorez.mods.skins.impl;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import vorez.lib.URLConnectionValidator;
import vorez.lib.URLValidator;
import vorez.mods.skins.impl.specifications.CustomServersList;
import vorez.mods.skins.impl.specifications.URLCheck;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.util.concurrent.CompletableFuture;

public final class YaclSettings {

    private YaclSettings() {
    }

    private static void showCheckResult(String title, URLCheck result) {
        Minecraft client = Minecraft.getInstance();

        Component message = switch (result) {
            case HTTP_DENIED ->
                Component.translatable("use.of.http.is.denied");

            case CUSTOM_SERVER_DISABLE ->
                Component.translatable("error.custom-server-disabled");

            case IS_EXAMPLE_COM ->
                    Component.translatable("toast.offlineskins.example");

            case SUCCESS, STABLE_CONNECTION ->
                    Component.translatable("toast.offlineskins.success");

            case UNSTABLE_CONNECTION ->
                    Component.translatable("toast.offlineskins.unstable");

            case FAIL ->
                    Component.translatable("toast.offlineskins.fail");

            case INVALID_URL ->
                    Component.translatable("toast.offlineskins.invalid_url");

            case ERROR_404 ->
                    Component.translatable("toast.offlineskins.error404");

            case OFFLINE ->
                    Component.translatable("toast.offlineskins.offline");

            case NO_RESPONSE ->
                    Component.translatable("toast.offlineskins.no_response");

            case INVALID_SKIN ->
                    Component.translatable("toast.offlineskins.invalid_skin");

            case INVALID_CAPE ->
                    Component.translatable("toast.offlineskins.invalid_cape");
        };

        SystemToast.add(
                client.gui.toastManager(),
                SystemToast.SystemToastId.FILE_DROP_FAILURE,
                Component.literal(title),
                message
        );
    }

    public static Screen createConfigScreen(Screen parentScreen) {
        ConfigOptions options = FabricOfflineSkinsReloaded.loadConfigSnapshot();
        ConfigOptions defaults = new ConfigOptions().defaultOptions();

        if (options.customServersList != CustomServersList.CUSTOM) {
            options.linkCustomServerSkin = options.customServersList.getSkinUrl();
            options.linkCustomServerCape = options.customServersList.getCapeUrl();
        }

        Option<Boolean> disablePlayerHeads = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.DisablePlayerHeads"))
                .description(OptionDescription.of(Component.translatable("tooltip.DisablePlayerHeads")))
                .binding(
                        defaults.disablePlayerHeads,
                        () -> options.disablePlayerHeads,
                        value -> options.disablePlayerHeads = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Boolean> useMojang = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.Mojang"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.Mojang")))
                .binding(
                        defaults.useMojang,
                        () -> options.useMojang,
                        value -> options.useMojang = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Boolean> useCrafatar = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.Crafatar"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.Crafatar")))
                .binding(
                        defaults.useCrafatar,
                        () -> options.useCrafatar,
                        value -> options.useCrafatar = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Boolean> allowHdSkins = Option.<Boolean>createBuilder()
                .name(Component.translatable("allow.HD"))
                .description(OptionDescription.of(Component.translatable("tooltip.allow.HD")))
                .binding(
                        defaults.allowHdSkins,
                        () -> options.allowHdSkins,
                        value -> options.allowHdSkins = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Boolean> useCustomServer = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.CustomServer"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.CustomServer")))
                .binding(
                        defaults.useCustomServer,
                        () -> options.useCustomServer,
                        value -> options.useCustomServer = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<CustomServersList> customServerPreset = Option.<CustomServersList>createBuilder()
                .name(Component.translatable("option.use.server.from.list"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.server.from.list")))
                .binding(
                        defaults.customServersList,
                        () -> options.customServersList,
                        preset -> {
                            options.customServersList = preset;
                            if (preset != CustomServersList.CUSTOM) {
                                options.linkCustomServerSkin = preset.getSkinUrl();
                                options.linkCustomServerCape = preset.getCapeUrl();
                            }
                        }
                )
                .controller(option -> EnumControllerBuilder.create(option).enumClass(CustomServersList.class))
                .build();
        
        Option<Boolean> allowHTTP = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.allowHTTP"))
                .description(OptionDescription.of(Component.translatable("tooltip.allowHTTP")))
                .binding(
                        defaults.allowHTTP,
                        () -> options.allowHTTP,
                        value -> options.allowHTTP = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        ButtonOption reloadConfig = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.reload"))
                .text(Component.translatable("button.offlineskins.reload"))
                .description(OptionDescription.of(
                        Component.translatable("tooltip.offlineskins.reload")
                ))
                .action((screen, option) -> {
                    FabricOfflineSkinsReloaded.reloadRuntime();

                    CompletableFuture.delayedExecutor(
                            1,
                            java.util.concurrent.TimeUnit.SECONDS
                    ).execute(() -> Minecraft.getInstance().execute(() ->
                            SystemToast.add(
                                    Minecraft.getInstance().gui.toastManager(),
                                    SystemToast.SystemToastId.FILE_DROP_FAILURE,
                                    Component.translatable("button.offlineskins.reload"),
                                    Component.translatable("toast.offlineskins.reload.success")
                            )
                    ));
                })
                .build();

        Option<String> customServerSkinUrl = Option.<String>createBuilder()
                .name(Component.translatable("option.offlineskins-reloaded.link_custom_server_skin"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins-reloaded.link_custom_server_skin")))
                .binding(
                        defaults.linkCustomServerSkin,
                        () -> options.linkCustomServerSkin,
                        value -> options.linkCustomServerSkin = value
                )
                .controller(StringControllerBuilder::create)
                .build();

        Option<String> customServerCapeUrl = Option.<String>createBuilder()
                .name(Component.translatable("option.offlineskins-reloaded.link_custom_server_cape"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins-reloaded.link_custom_server_cape")))
                .binding(
                        defaults.linkCustomServerCape,
                        () -> options.linkCustomServerCape,
                        value -> options.linkCustomServerCape = value
                )
                .controller(StringControllerBuilder::create)
                .build();

        ButtonOption checkSkin = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.check_skin"))
                .text(Component.translatable("button.offlineskins.check"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.check_skin")))
                .action((screen, option) -> CompletableFuture
                        .supplyAsync(() -> {
                            URLCheck local = URLValidator.validate(options.linkCustomServerSkin, true);
                            if (local != URLCheck.SUCCESS) {
                                return local;
                            }
                            return URLConnectionValidator.checkConnection(
                                    options.linkCustomServerSkin,
                                    true,
                                    options.useCustomServer,
                                    false
                            );
                        })
                        .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                            URLCheck status = error == null && result != null
                                    ? result
                                    : URLCheck.NO_RESPONSE;

                            showCheckResult("Skin", status);
                        }))
                )
                .build();

        ButtonOption checkCape = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.check_cape"))
                .text(Component.translatable("button.offlineskins.check"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.check_cape")))
                .action((screen, option) -> CompletableFuture
                        .supplyAsync(() -> {
                            URLCheck local = URLValidator.validate(options.linkCustomServerCape, true);
                            if (local != URLCheck.SUCCESS) {
                                return local;
                            }
                            return URLConnectionValidator.checkConnection(
                                    options.linkCustomServerCape,
                                    true,
                                    options.useCustomServer,
                                    true
                            );
                        })
                        .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                            URLCheck status = error == null && result != null
                                    ? result
                                    : URLCheck.NO_RESPONSE;

                            showCheckResult("Cape", status);
                        }))
                )
                .build();

        OptionGroup generalGroup = OptionGroup.createBuilder()
                .name(Component.translatable("category.offlineskins-reloaded.general"))
                .collapsed(false)
                .option(disablePlayerHeads)
                .option(useMojang)
                .option(useCrafatar)
                .build();

        OptionGroup customServerGroup = OptionGroup.createBuilder()
                .name(Component.translatable("options.CustomServer"))
                .collapsed(true)
                .option(useCustomServer)
                .option(allowHTTP)
                .option(allowHdSkins)
                .option(reloadConfig)
                .option(customServerPreset)
                .option(customServerSkinUrl)
                .option(checkSkin)
                .option(customServerCapeUrl)
                .option(checkCape)
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("menu.offlineskins-reloaded.config"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("menu.offlineskins-reloaded.config"))
                        .group(generalGroup)
                        .group(customServerGroup)
                        .build())
                .save(() -> {
                    FabricOfflineSkinsReloaded.saveConfigFile(options);
                    FabricOfflineSkinsReloaded.reloadRuntime();
                })
                .build()
                .generateScreen(parentScreen);
    }
}