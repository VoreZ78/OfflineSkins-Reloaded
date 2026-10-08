package vorez.mods.skins.impl;

import com.mojang.logging.LogUtils;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.gui.controllers.LabelController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import vorez.mods.skins.api.SkinProviderAPI;
import vorez.mods.skins.impl.CustomServersList.CustomServersList;
import vorez.mods.skins.impl.Utils.SkinUtils;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import vorez.mods.skins.providers.CachedCapeProvider;
import vorez.mods.skins.providers.CachedSkinProvider;
import vorez.network.URLConnectionValidator;
import vorez.network.URLStatus;
import vorez.network.URLValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class YaclSettings {

    private static final Logger LOG = LogUtils.getLogger();

    private YaclSettings() {
    }

    private static CustomServersList currentPreset = CustomServersList.CUSTOM;

    public static void setCurrentPreset(CustomServersList preset) {
        currentPreset = preset;
    }

    public static CustomServersList getCurrentPreset() {
        return currentPreset;
    }

    private static String skinUrl = "";
    private static String capeUrl = "";

    public static void setSkinUrl(String value) {
        skinUrl = value;
    }
    public static String getSkinUrl() {
        return skinUrl;
    }
    public static void setCapeUrl(String value) {
        capeUrl = value;
    }
    public static String getCapeUrl() {
        return capeUrl;
    }

    public static String prepareGitHub(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        if (url.startsWith("http")) {
            return url;
        }

        return "https://raw.githubusercontent.com/" + url;
    }

    private static List<String> scanCachedImages(Path directory) {
        List<String> result = new ArrayList<>();

        if (!Files.exists(directory)) {
            return result;
        }

        try (var paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString()
                            .toLowerCase(java.util.Locale.ROOT)
                            .endsWith(".png"))
                    .map(path -> directory.relativize(path).toString()
                            .replace('\\', '/'))
                    .sorted(Comparator.naturalOrder())
                    .forEach(result::add);
        } catch (IOException e) {
            LOG.error("[OfflineSkins Reloaded] Failed to scan cachedImages.", e);
        }

        return result;
    }

    public static Screen createConfigScreen(Screen parentScreen) {
        ConfigOptions options = FabricOfflineSkinsReloaded.loadConfigSnapshot();
        ConfigOptions defaults = new ConfigOptions().defaultOptions();

        setCurrentPreset(options.customServersList);

        if (options.customServersList.isElyBy()) {
            options.linkCustomServerSkin = options.customServersList.getSkinUrl();
            options.linkCustomServerCape = options.customServersList.getCapeUrl();
        }

        if (options.customServersList.isGithub()) {
            String prefix = "https://raw.githubusercontent.com/";

            if (options.linkCustomServerSkin.startsWith(prefix)) {
                options.linkCustomServerSkin = options.linkCustomServerSkin.substring(prefix.length());
            }

            if (options.linkCustomServerCape.startsWith(prefix)) {
                options.linkCustomServerCape = options.linkCustomServerCape.substring(prefix.length());
            }
        }

        setSkinUrl(options.linkCustomServerSkin);
        setCapeUrl(options.linkCustomServerCape);

        Minecraft client = Minecraft.getInstance();

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

        Option<Boolean> smartInternetCheck = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.SmartInternetCheck"))
                .description(OptionDescription.of(Component.translatable("tooltip.SmartInternetCheck")))
                .binding(
                        defaults.smartInternetCheck,
                        () -> options.smartInternetCheck,
                        value -> options.smartInternetCheck = value
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

        ButtonOption reloadProviders = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.reload"))
                .text(Component.translatable("button.offlineskins.reload.t"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.reload")))
                .action((screen, option) -> {
                    FabricOfflineSkinsReloaded.reloadRuntime();

                    CompletableFuture.delayedExecutor(
                            1,
                            java.util.concurrent.TimeUnit.SECONDS
                    ).execute(() -> client.execute(() ->
                            client.getToastManager().addToast(
                                    SystemToast.multiline(
                                            client,
                                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                            Component.translatable("offlineskins.reload.notification"),
                                            Component.translatable("toast.offlineskins.reload.success")
                                    )
                            )
                    ));
                })
                .build();

        Option<Boolean> useCustomServer = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.use.CustomServer"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.CustomServer")))
                .binding(
                        defaults.useCustomServer,
                        () -> options.useCustomServer,
                        value -> options.useCustomServer = value
                )
                .controller(TickBoxControllerBuilder::create)
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

        Option<Boolean> allowHDSkins = Option.<Boolean>createBuilder()
                .name(Component.translatable("allow.HD"))
                .description(OptionDescription.of(Component.translatable("tooltip.allow.HD")))
                .binding(
                        defaults.allowHDPlayers,
                        () -> options.allowHDPlayers,
                        value -> options.allowHDPlayers = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Integer> HDImagesResolution = Option.<Integer>createBuilder()
                .name(Component.translatable("offlineskins-reloaded.hd_images_resolution"))
                .description(OptionDescription.of(
                        Component.translatable("offlineskins-reloaded.hd_images_resolution.d")
                ))
                .binding(
                        128,
                        () -> options.maxHDResolution,
                        value -> options.maxHDResolution = value
                )
                .controller(resolution -> CyclingListControllerBuilder.create(resolution)
                        .values(128, 256, 512, 1024, 2048, 4096, -1)
                        .formatValue(value -> {
                            if (value == -1)
                                return Component.translatable("offlineskins-reloaded.anyHDResolution");

                            return Component.literal(value + "x" + value);
                        }))
                .build();

        Option<String> customServerSkinUrl = Option.<String>createBuilder()
                .name(Component.translatable("option.offlineskins-reloaded.link_custom_server_skin"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins-reloaded.link_custom_server_skin")))
                .binding(
                        defaults.linkCustomServerSkin,
                        () -> options.linkCustomServerSkin,
                        value -> {
                            if (!options.customServersList.isElyBy()) {
                                options.linkCustomServerSkin = value;
                                setSkinUrl(value);
                            }
                        }
                )
                .controller(StringControllerBuilder::create)
                .build();

        Option<String> customServerCapeUrl = Option.<String>createBuilder()
                .name(Component.translatable("option.offlineskins-reloaded.link_custom_server_cape"))
                .description(OptionDescription.of(
                        Component.translatable("tooltip.offlineskins-reloaded.link_custom_server_cape")
                ))
                .binding(
                        defaults.linkCustomServerCape,
                        () -> options.linkCustomServerCape,
                        value -> {
                            if (!options.customServersList.isElyBy()) {
                                options.linkCustomServerCape = value;
                                setCapeUrl(value);
                            }
                        }
                )
                .controller(StringControllerBuilder::create)
                .build();

        Option<CustomServersList> customServerPreset = Option.<CustomServersList>createBuilder()
                .name(Component.translatable("option.use.server.from.list"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.server.from.list")))
                .binding(
                        defaults.customServersList,
                        () -> options.customServersList,
                        preset -> {
                            options.customServersList = preset;
                            setCurrentPreset(preset);

                            if (preset.isElyBy()) {
                                options.linkCustomServerSkin = preset.getSkinUrl();
                                options.linkCustomServerCape = preset.getCapeUrl();

                                customServerSkinUrl.requestSet(options.linkCustomServerSkin);
                                customServerCapeUrl.requestSet(options.linkCustomServerCape);

                                setSkinUrl(options.linkCustomServerSkin);
                                setCapeUrl(options.linkCustomServerCape);
                            }
                        }
                )
                .controller(option -> EnumControllerBuilder.create(option)
                        .enumClass(CustomServersList.class))
                .build();

        ButtonOption checkSkin = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.check_skin"))
                .text(Component.translatable("button.offlineskins.check"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.check_skin")))
                .action((screen, option) -> CompletableFuture
                        .supplyAsync(() -> {
                            String url = options.linkCustomServerSkin;

                            if (options.customServersList.isGithub()) {
                                url = prepareGitHub(url);
                            }

                            URLStatus local = URLValidator.validate(url, true);

                            if (local != URLStatus.SUCCESS) {
                                return local;
                            }

                            return URLConnectionValidator.checkConnection(
                                    url,
                                    options.useCustomServer,
                                    false
                            );
                        })
                        .whenComplete((result, error) ->
                                client.execute(() -> {
                                    URLStatus status = error == null && result != null
                                            ? result
                                            : URLStatus.NO_RESPONSE;

                                    URLValidator.showCheckResult("Skin", status);
                                }))
                )
                .build();

        ButtonOption checkCape = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.check_cape"))
                .text(Component.translatable("button.offlineskins.check"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.check_cape")))
                .action((screen, option) -> CompletableFuture
                        .supplyAsync(() -> {
                            String url = options.linkCustomServerCape;

                            if (options.customServersList.isGithub()) {
                                url = prepareGitHub(url);
                            }

                            URLStatus local = URLValidator.validate(url, true);

                            if (local != URLStatus.SUCCESS) {
                                return local;
                            }

                            return URLConnectionValidator.checkConnection(
                                    url,
                                    options.useCustomServer,
                                    true
                            );
                        })
                        .whenComplete((result, error) ->
                                client.execute(() -> {
                                    URLStatus status = error == null && result != null
                                            ? result
                                            : URLStatus.NO_RESPONSE;

                                    URLValidator.showCheckResult("Cape", status);
                                }))
                )
                .build();

        ButtonOption openDirectoryCachedImages = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.open_cachedimages"))
                .text(Component.translatable("button.offlineskins.open_cachedimages.t"))
                .description(OptionDescription.of(
                        Component.translatable("tooltip.offlineskins.open_cachedimages")))
                .action((screen, option) -> {
                    Path path = Paths.get(".", "cachedImages");
                    Path skins = path.resolve("skins");
                    Path skinsUuid = skins.resolve("uuid");
                    Path capes = path.resolve("capes");
                    Path capesUuid = capes.resolve("uuid");

                    try {
                        Files.createDirectories(skinsUuid);
                        Files.createDirectories(capesUuid);

                        Util.getPlatform().openPath(path);
                    } catch (IOException e) {
                        LOG.error("[OfflineSkins Reloaded] Failed to create/open cachedImages directories.", e);
                    }
                })
                .build();

        ButtonOption recacheSkin = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.recacheSkin"))
                .text(Component.translatable("button.offlineskins.recacheSkin.t"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.recacheSkin")))
                .action((screen, option) ->
                        FabricOfflineSkinsReloaded.recacheSkin())
                .build();

        ButtonOption recacheCape = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.recacheCape"))
                .text(Component.translatable("button.offlineskins.recacheCape.t"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.recacheCape")))
                .action((screen, option) ->
                        FabricOfflineSkinsReloaded.recacheCape())
                .build();

        Option<Integer> AppearanceDelay = Option.<Integer>createBuilder()
                .name(Component.translatable("offlineskins-reloaded.skin.cape_appearance_delay"))
                .description(OptionDescription.of(Component.translatable("offlineskins-reloaded.skin.cape_appearance_delay.d")))
                .binding(
                        1000,
                        () -> options.appearanceDelay,
                        value -> options.appearanceDelay = value
                )
                .controller(delay -> IntegerSliderControllerBuilder.create(delay)
                        .range(0, 3000)
                        .step(100)
                        .formatValue(value -> {
                            if (value == 0)
                                return Component.translatable("instant.delay");
                            return Component.literal(value + " ms");
                        }))
                .build();

        ButtonOption refreshScreen = ButtonOption.createBuilder()
                .name(Component.translatable("offlineskins-reloaded.YACL.refresh.screen"))
                .description(OptionDescription.of(Component.translatable("offlineskins-reloaded.YACL.refresh.screen.d")))
                .text(Component.translatable("offlineskins-reloaded.YACL.refresh.screen.t"))
                .action((screen, option) -> client.setScreen(
                        createConfigScreen(parentScreen)
                ))
                .build();

        Path cachedImages = Paths.get(".", "cachedImages");

        List<String> cachedSkins = scanCachedImages(
                cachedImages.resolve("skins")
        );

        Option<Boolean> useCachedSkin = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.use.cachedSkin.players"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.cachedSkin.players")))
                .binding(
                        defaults.useCachedSkin,
                        () -> options.useCachedSkin,
                        value -> options.useCachedSkin = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Boolean> rememberSkin = Option.<Boolean>createBuilder()
                .name(Component.translatable("remember.skin.layer.player"))
                .description(OptionDescription.of(Component.translatable("remember.skin.layer.player.d")))
                .binding(
                        defaults.rememberSkin,
                        () -> options.rememberSkin,
                        value -> options.rememberSkin = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        OptionGroup cachedSkinsOptions;

        if (!cachedSkins.isEmpty()) {
            OptionGroup.Builder cachedSkinsGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("category.offlineskins-reloaded.cachedSkins"))
                    .collapsed(false)
                    .option(useCachedSkin)
                    .option(rememberSkin);

            for (String skin : cachedSkins) {
                Path skinPath = cachedImages.resolve("skins").resolve(skin);

                boolean selected = CachedSkinProvider.isSelectedSkin(skin);

                ButtonOption skinOption = ButtonOption.createBuilder()
                        .name(Component.literal(skin))
                        .text(Component.translatable(
                                selected
                                        ? "button.offlineskins.selected"
                                        : "button.offlineskins.select"
                        ))
                        .description(OptionDescription.of(Component.translatable("use.this.skin")))
                        .action((screen, option) -> {
                            if (client.player != null) {
                                String currentProfileName = PlayerProfile
                                        .wrapGameProfile(client.player.getGameProfile())
                                        .getPlayerName();

                                CachedSkinProvider.setSelectedSkin(
                                        currentProfileName,
                                        skinPath
                                );

                                FabricOfflineSkinsReloaded.recacheSkin();
                            }
                        })
                        .build();

                cachedSkinsGroup.option(skinOption);
            }

            ButtonOption resetSkin = ButtonOption.createBuilder()
                    .name(Component.translatable("clear.skin.cache.player"))
                    .description(OptionDescription.of(Component.translatable("clear.skin.cache.player.d")))
                    .text(Component.translatable("clear.skin.cape.cache.player.t"))
                    .action((screen, option) -> {
                        if (client.player != null) {
                            PlayerProfile profile = PlayerProfile.wrapGameProfile(client.player.getGameProfile());
                            String playerName = profile.getPlayerName();

                            CachedSkinProvider.resetSkin(playerName);

                            SkinProviderAPI.SKIN.clearFirst(profile);

                            SkinUtils.clearPlayerTextureSuppliers(playerName);
                        }
                    })
                    .build();

            cachedSkinsGroup.option(resetSkin);
            cachedSkinsOptions = cachedSkinsGroup.build();
        } else {
            OptionGroup.Builder cachedSkinsGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("cached.images.empty"))
                    .collapsed(false);

            Option<Component> emptyList = Option.<Component>createBuilder()
                    .name(Component.empty())
                    .description(OptionDescription.EMPTY)
                    .stateManager(StateManager.createImmutable(
                            Component.translatable("empty.list.skins")
                    ))
                    .customController(LabelController::new)
                    .build();

            cachedSkinsGroup.option(emptyList);
            cachedSkinsOptions = cachedSkinsGroup.build();
        }

        List<String> cachedCapes = scanCachedImages(
                cachedImages.resolve("capes")
        );

        Option<Boolean> useCachedCape = Option.<Boolean>createBuilder()
                .name(Component.translatable("options.use.cachedCape.players"))
                .description(OptionDescription.of(Component.translatable("tooltip.use.cachedCape.players")))
                .binding(
                        defaults.useCachedCape,
                        () -> options.useCachedCape,
                        value -> options.useCachedCape = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Boolean> rememberCape = Option.<Boolean>createBuilder()
                .name(Component.translatable("remember.cape.layer.player"))
                .description(OptionDescription.of(Component.translatable("remember.cape.layer.player.d")))
                .binding(
                        defaults.rememberCape,
                        () -> options.rememberCape,
                        value -> options.rememberCape = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        OptionGroup cachedCapesOptions;

        if (!cachedCapes.isEmpty()) {
            OptionGroup.Builder cachedCapesGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("category.offlineskins-reloaded.cachedCapes"))
                    .collapsed(false)
                    .option(useCachedCape)
                    .option(rememberCape);

            for (String cape : cachedCapes) {
                Path capePath = cachedImages.resolve("capes").resolve(cape);

                boolean selected = CachedCapeProvider.isSelectedCape(cape);

                ButtonOption capeOption = ButtonOption.createBuilder()
                        .name(Component.literal(cape))
                        .text(Component.translatable(
                                selected
                                        ? "button.offlineskins.selected"
                                        : "button.offlineskins.select"
                        ))
                        .description(OptionDescription.of(Component.translatable("use.this.cape")))
                        .action((screen, option) -> {
                            if (client.player != null) {
                                String currentProfileName = PlayerProfile
                                        .wrapGameProfile(client.player.getGameProfile())
                                        .getPlayerName();

                                CachedCapeProvider.setSelectedCape(
                                        currentProfileName,
                                        capePath
                                );

                                FabricOfflineSkinsReloaded.recacheCape();
                            }
                        })
                        .build();

                cachedCapesGroup.option(capeOption);
            }

            ButtonOption resetCape = ButtonOption.createBuilder()
                    .name(Component.translatable("clear.cape.cache.player"))
                    .description(OptionDescription.of(Component.translatable("clear.cape.cache.player.d")))
                    .text(Component.translatable("clear.skin.cape.cache.player.t"))
                    .action((screen, option) -> {
                        if (client.player != null) {
                            PlayerProfile profile = PlayerProfile.wrapGameProfile(client.player.getGameProfile());
                            String playerName = profile.getPlayerName();

                            CachedCapeProvider.resetCape(playerName);

                            SkinProviderAPI.CAPE.clearFirst(profile);

                            SkinUtils.clearPlayerTextureSuppliers(playerName);
                        }
                    })
                    .build();

            cachedCapesGroup.option(resetCape);
            cachedCapesOptions = cachedCapesGroup.build();
        } else {
            OptionGroup.Builder cachedCapesGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("cached.images.empty"))
                    .collapsed(false);

            Option<Component> emptyList = Option.<Component>createBuilder()
                    .name(Component.empty())
                    .description(OptionDescription.EMPTY)
                    .stateManager(StateManager.createImmutable(
                            Component.translatable("empty.list.capes")
                    ))
                    .customController(LabelController::new)
                    .build();

            cachedCapesGroup.option(emptyList);
            cachedCapesOptions = cachedCapesGroup.build();
        }

        OptionGroup.Builder copyLinksGroup = OptionGroup.createBuilder()
                .name(Component.translatable("category.offlineskins-reloaded.copyLinks"))
                .collapsed(true);

        for (int i = 0; i < options.copyLinks.length; i++) {
            int index = i;

            Option<String> copyLink = Option.<String>createBuilder()
                    .name(Component.translatable("copy.link.from.list", index + 1))
                    .description(OptionDescription.of(Component.translatable("copy.link.from.list.d", index + 1)))
                    .binding(
                            defaults.copyLinks[index],
                            () -> options.copyLinks[index],
                            value -> options.copyLinks[index] = value
                    )
                    .controller(StringControllerBuilder::create)
                    .build();

            copyLinksGroup.option(copyLink);
        }

        OptionGroup copyLinks = copyLinksGroup.build();

        Option<Boolean> smoothRecache = Option.<Boolean>createBuilder()
                .name(Component.translatable("smoothInitialization.recache.players"))
                .description(OptionDescription.of(Component.translatable("smoothInitialization.recache.players.d")))
                .binding(
                        defaults.smoothInitialization,
                        () -> options.smoothInitialization,
                        value -> options.smoothInitialization = value
                )
                .controller(TickBoxControllerBuilder::create)
                .build();

        ButtonOption recacheSkins = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.recacheSkin.players"))
                .text(Component.translatable("button.offlineskins.recacheSkin.players.t"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.recacheSkin.players")))
                .action((screen, option) ->
                        FabricOfflineSkinsReloaded.recacheSkins())
                .build();

        ButtonOption recacheCapes = ButtonOption.createBuilder()
                .name(Component.translatable("button.offlineskins.recacheCape.players"))
                .text(Component.translatable("button.offlineskins.recacheCape.players.t"))
                .description(OptionDescription.of(Component.translatable("tooltip.offlineskins.recacheCape.players")))
                .action((screen, option) ->
                        FabricOfflineSkinsReloaded.recacheCapes())
                .build();

        OptionGroup NetworkGeneralGroup = OptionGroup.createBuilder()
                .name(Component.translatable("category.offlineskins-reloaded.general"))
                .collapsed(false)
                .option(disablePlayerHeads)
                .option(smartInternetCheck)
                .option(useMojang)
                .option(useCrafatar)
                .option(reloadProviders)
                .build();

        OptionGroup customServerGroup = OptionGroup.createBuilder()
                .name(Component.translatable("options.CustomServer"))
                .collapsed(true)
                .option(useCustomServer)
                .option(allowHTTP)
                .option(allowHDSkins)
                .option(HDImagesResolution)
                .option(reloadProviders)
                .option(customServerPreset)
                .option(customServerSkinUrl)
                .option(checkSkin)
                .option(customServerCapeUrl)
                .option(checkCape)
                .build();

        OptionGroup recacheGroup = OptionGroup.createBuilder()
                .name(Component.translatable("options.recache"))
                .collapsed(false)
                .option(smoothRecache)
                .option(recacheSkins)
                .option(recacheCapes)
                .build();

        OptionGroup OfflineSkinsReloadedMain = OptionGroup.createBuilder()
                .name(Component.translatable("category.offlineskins-reloaded.dressing.room"))
                .collapsed(false)
                .option(openDirectoryCachedImages)
                .option(recacheSkin)
                .option(recacheCape)
                .option(AppearanceDelay)
                .build();

        OptionGroup updateYACLScreen = OptionGroup.createBuilder()
                .name(Component.translatable("offlineskins-reloaded.YACL.refresh.screen.Group"))
                .option(refreshScreen)
                .build();

        ConfigCategory dressingRoomCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("menu.offlineskins-reloaded.dressing.room"))
                .group(OfflineSkinsReloadedMain)
                .group(updateYACLScreen)
                .group(cachedSkinsOptions)
                .group(cachedCapesOptions)
                .build();

        ConfigCategory NetworkCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("menu.offlineskins-reloaded.network"))
                .group(NetworkGeneralGroup)
                .group(customServerGroup)
                .group(recacheGroup)
                .group(copyLinks)
                .build();

        ConfigCategory FAQCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("menu.faqs"))
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.skin.cape.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.skin.cape.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .build()
                )
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("menu.faqs.remember.skin.cape.title"))
                        .collapsed(true)
                        .option(Option.<Component>createBuilder()
                                .name(Component.empty())
                                .description(OptionDescription.EMPTY)
                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.remember.skin.cape.answer")))
                                .customController(LabelController::new)
                                .build()
                        )
                        .build()
                )
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.skin.cape.wrong.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.skin.cape.wrong.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .build()
                )
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.default.skin.cape.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.default.skin.cape.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .build()
                )
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.cached.skin.cape.can.see.others.players.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.cached.skin.cape.can.see.others.players.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .build()
                )
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.red.steve.cape.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.red.steve.cape.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .build()
                )
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.cache.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.cache.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .build()
                )
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable(
                                        "menu.faqs.server.title"
                                ))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.server.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .build()
                )
                .group(OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.links.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.links.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .option(ButtonOption.createBuilder()
                                                .name(Component.literal("Discord"))
                                                .text(Component.literal(""))
                                                .description(OptionDescription.of(Component.translatable("menu.faqs.discord.answer")))
                                                .action((screen, option) ->
                                                        ConfirmLinkScreen.confirmLinkNow(
                                                                screen,
                                                                "https://discord.gg/KgscJEE5B",
                                                                true
                                                        ))
                                                .build()
                                )
                                .option(ButtonOption.createBuilder()
                                                .name(Component.literal("Modrinth"))
                                                .text(Component.literal(""))
                                                .description(OptionDescription.of(Component.translatable("menu.faqs.modrinth.answer")))
                                                .action((screen, option) ->
                                                        ConfirmLinkScreen.confirmLinkNow(
                                                                screen,
                                                                "https://modrinth.com/mod/offlineskins-reloaded",
                                                                true
                                                        ))
                                                .build()
                                )
                                .option(ButtonOption.createBuilder()
                                                .name(Component.literal("GitHub"))
                                                .text(Component.literal(""))
                                                .description(OptionDescription.of(Component.translatable("menu.faqs.github.answer")))
                                                .action((screen, option) ->
                                                        ConfirmLinkScreen.confirmLinkNow(
                                                                screen,
                                                                "https://github.com/VoreZ78/OfflineSkins-Reloaded",
                                                                true
                                                        ))
                                                .build()
                                )
                                .build()
                )
                .group(
                        OptionGroup.createBuilder()
                                .name(Component.translatable("menu.faqs.bug.report.title"))
                                .collapsed(true)
                                .option(Option.<Component>createBuilder()
                                                .name(Component.empty())
                                                .description(OptionDescription.EMPTY)
                                                .stateManager(StateManager.createImmutable(Component.translatable("menu.faqs.bug.report.answer")))
                                                .customController(LabelController::new)
                                                .build()
                                )
                                .option(ButtonOption.createBuilder()
                                                .name(Component.literal("GitHub Issues"))
                                                .text(Component.literal(""))
                                                .description(OptionDescription.of(Component.translatable("menu.faqs.github.answer")))
                                                .action((screen, option) ->
                                                        ConfirmLinkScreen.confirmLinkNow(
                                                                screen,
                                                                "https://github.com/VoreZ78/OfflineSkins-Reloaded/issues",
                                                                true
                                                        ))
                                                .build()
                                )
                                .option(ButtonOption.createBuilder()
                                                .name(Component.literal("Discord"))
                                                .text(Component.literal(""))
                                                .description(OptionDescription.of(Component.translatable("menu.faqs.discord.answer")))
                                                .action((screen, option) ->
                                                        ConfirmLinkScreen.confirmLinkNow(
                                                                screen,
                                                                "https://discord.gg/KgscJEE5B",
                                                                true
                                                        ))
                                                .build()
                                )
                                .build()
                )
                .build();

        ConfigCategory debugCategory = ConfigCategory.createBuilder()
                .name(Component.literal("Debug"))
                .group(OptionGroup.createBuilder()
                                .name(Component.literal("Debug"))
                                .collapsed(false)
                                .option(
                                        Option.<Boolean>createBuilder()
                                                .name(Component.translatable("options.offlineskins-reloaded.debug.options"))
                                                .description(OptionDescription.of(Component.translatable("options.offlineskins-reloaded.debug.options.description")))
                                                .binding(
                                                        defaults.logInfo,
                                                        () -> options.logInfo,
                                                        value -> options.logInfo = value
                                                )
                                                .controller(TickBoxControllerBuilder::create)
                                                .build()
                                )
                                .build()
                )
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal(""))
                .category(dressingRoomCategory)
                .category(NetworkCategory)
                .category(FAQCategory)
                .category(debugCategory)
                .save(() -> {
                    if (options.customServersList.isElyBy()) {
                        options.linkCustomServerSkin = options.customServersList.getSkinUrl();
                        options.linkCustomServerCape = options.customServersList.getCapeUrl();
                    }
                    if (options.customServersList.isGithub()) {
                        options.linkCustomServerSkin = prepareGitHub(options.linkCustomServerSkin);
                        options.linkCustomServerCape = prepareGitHub(options.linkCustomServerCape);
                    }

                    FabricOfflineSkinsReloaded.saveConfigFile(options);
                    FabricOfflineSkinsReloaded.reloadRuntime();
                })
                .build()
                .generateScreen(parentScreen);
    }
}