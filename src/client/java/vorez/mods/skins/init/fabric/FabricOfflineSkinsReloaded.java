package vorez.mods.skins.init.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import vorez.mods.skins.api.SkinProviderAPI;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.impl.ConfigOptions;
import vorez.mods.skins.impl.KeyBindsAndCommands;
import vorez.mods.skins.impl.PlayerProfile;
import vorez.mods.skins.impl.Utils.ImageUtils;
import vorez.mods.skins.impl.Utils.SkinUtils;
import vorez.mods.skins.providers.*;
import vorez.network.SmartInternetCheck;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FabricOfflineSkinsReloaded implements ClientModInitializer {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = Paths.get(".", "config", "offlineskins-reloaded.json");
    private static final Map<String, ResourceLocation> textures = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> skinAppearanceDelayUntil = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> capeAppearanceDelayUntil = new ConcurrentHashMap<>();


    public static boolean PLAYERHEADS = true;

    private static volatile ConfigOptions lastLoadedConfig = new ConfigOptions().defaultOptions();

    private static ResourceLocation generateRandomLocation() {
        return ResourceLocation.fromNamespaceAndPath("offlineskins-reloaded", String.format("textures/generated/%s", UUID.randomUUID()));
    }

    private static String textureKey(ByteBuffer data) {
        if (data == null) {
            return null;
        }
        ByteBuffer copy = data.asReadOnlyBuffer();
        copy.rewind();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static ResourceLocation getLocationSkin(GameProfile profile) {
        if (isSkinAppearanceDelayed(profile)) {
            return null;
        }

        ISkin skin = SkinProviderAPI.SKIN.getSkin(PlayerProfile.wrapGameProfile(profile));
        if (skin != null && skin.isDataReady()) {
            ByteBuffer data = skin.getData();
            if (data != null) {
                return getOrCreateTextureNullable(data, skin);
            }
        }
        return null;
    }
    public static ResourceLocation getLocationCape(GameProfile profile) {
        if (isCapeAppearanceDelayed(profile)) {
            return null;
        }

        ISkin skin = SkinProviderAPI.CAPE.getSkin(PlayerProfile.wrapGameProfile(profile));
        if (skin != null && skin.isDataReady()) {
            ByteBuffer data = skin.getData();
            if (data != null) {
                return getOrCreateTextureNullable(data, skin);
            }
        }
        return null;
    }

    public static ResourceLocation getUnofficialLocationSkin(GameProfile profile) {
        if (isSkinAppearanceDelayed(profile)) {
            return null;
        }

        ISkin skin = SkinProviderAPI.SKIN.getUnofficialSkin(PlayerProfile.wrapGameProfile(profile));
        if (skin != null && skin.isDataReady()) {
            ByteBuffer data = skin.getData();
            if (data != null) {
                return getOrCreateTextureNullable(data, skin);
            }
        }
        return null;
    }

    private static void delaySkinAppearance(GameProfile profile) {
        int delay = lastLoadedConfig.appearanceDelay;

        if (delay <= 0) {
            skinAppearanceDelayUntil.remove(profile.id());
            return;
        }

        skinAppearanceDelayUntil.put(
                profile.id(),
                System.currentTimeMillis() + delay
        );
    }

    private static void delayCapeAppearance(GameProfile profile) {
        int delay = lastLoadedConfig.appearanceDelay;

        if (delay <= 0) {
            capeAppearanceDelayUntil.remove(profile.id());
            return;
        }

        capeAppearanceDelayUntil.put(
                profile.id(),
                System.currentTimeMillis() + delay
        );
    }

    private static long smoothInitialization() {
        if (!lastLoadedConfig.smoothInitialization) {
            return 0;
        }

        return (long) ((Math.random()) * 1000);
    }

    private static void runSmoothInitialization(long delay, Runnable action) {
        if (delay <= 0) {
            action.run();
            return;
        }

        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                .execute(() -> Minecraft.getInstance().execute(action));
    }

    private static boolean isSkinAppearanceDelayed(GameProfile profile) {
        Long until = skinAppearanceDelayUntil.get(profile.id());

        if (until == null)
            return false;

        if (System.currentTimeMillis() >= until) {
            skinAppearanceDelayUntil.remove(profile.id(), until);
            return false;
        }

        return true;
    }

    private static boolean isCapeAppearanceDelayed(GameProfile profile) {
        Long until = capeAppearanceDelayUntil.get(profile.id());

        if (until == null)
            return false;

        if (System.currentTimeMillis() >= until) {
            capeAppearanceDelayUntil.remove(profile.id(), until);
            return false;
        }

        return true;
    }

    private static ResourceLocation registerTexture(ByteBuffer data, ISkin skin, String key) throws IOException {
        ResourceLocation location = generateRandomLocation();
        ByteBuffer readBuffer = data.asReadOnlyBuffer();
        readBuffer.rewind();
        DynamicTexture texture = new DynamicTexture(location::toString, NativeImage.read(readBuffer));
        Minecraft client = Minecraft.getInstance();
        client.getTextureManager().register(location, texture);
        textures.put(key, location);

        if (skin != null) {
            skin.setRemovalListener(s -> {
                ByteBuffer removedData = s.getData();
                if (removedData != null && key.equals(textureKey(removedData))) {
                    client.execute(() -> {
                        client.getTextureManager().release(location);
                        textures.remove(key, location);
                    });
                }
            });
        }
        return location;
    }

    private static ResourceLocation getOrCreateTexture(ByteBuffer data, ISkin skin) throws IOException {
        String key = textureKey(data);
        if (key == null) {
            return null;
        }

        ResourceLocation existing = textures.get(key);
        if (existing != null) {
            return existing;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) {
            return registerTexture(data, skin, key);
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ResourceLocation> result = new AtomicReference<>();
        AtomicReference<IOException> error = new AtomicReference<>();
        client.execute(() -> {
            try {
                result.set(registerTexture(data, skin, key));
            } catch (IOException e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while registering texture", e);
        }
        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }

    private static ResourceLocation getOrCreateTextureNullable(ByteBuffer data, ISkin skin) {
        try {
            return getOrCreateTexture(data, skin);
        } catch (IOException e) {
            return null;
        }
    }

    public static String getSkinType(GameProfile profile) {
        ResourceLocation location = getLocationSkin(profile);
        if (location != null) {
            ISkin skin = SkinProviderAPI.SKIN.getSkin(PlayerProfile.wrapGameProfile(profile));
            if (skin != null && skin.isDataReady()) {
                ByteBuffer data = skin.getData();
                if (data != null) {
                    return skin.getSkinType();
                }
            }
        }
        return null;
    }

    public static synchronized ConfigOptions loadConfigSnapshot() {
        ConfigOptions config = loadConfigFromDisk();
        lastLoadedConfig = config;
        return config;
    }

    public static synchronized void saveConfigFile(ConfigOptions config) {
        if (config == null) {
            config = new ConfigOptions().defaultOptions();
        }
        config.validate();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            LOG.error("[OfflineSkins-Reloaded] Failed to write config file.", e);
        }
    }

    public static ConfigOptions getRuntimeConfig() {
        return lastLoadedConfig;
    }

    public static synchronized void reloadRuntime() {
        ConfigOptions config = loadConfigSnapshot();
        applyConfig(config);

        if (config.smartInternetCheck)
            SmartInternetCheck.check();
        else
            SmartInternetCheck.reset();
    }

    private static ConfigOptions loadConfigFromDisk() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!Files.exists(CONFIG_PATH)) {
            ConfigOptions config = new ConfigOptions().defaultOptions();
            saveConfigFile(config);
            return config;
        }

        try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            JsonObject originalJson = JsonParser.parseString(json).getAsJsonObject();

            ConfigOptions config = GSON.fromJson(originalJson, ConfigOptions.class);

            if (config == null) {
                config = new ConfigOptions().defaultOptions();
            }

            config.hints();

            boolean changed = config.validate();

            JsonObject normalizedJson = GSON.toJsonTree(config).getAsJsonObject();

            if (changed || !originalJson.equals(normalizedJson)) {
                Files.writeString(
                        CONFIG_PATH,
                        GSON.toJson(normalizedJson),
                        StandardCharsets.UTF_8
                );
            }

            return config;

        } catch (Exception e) {
            LOG.error("[OfflineSkins-Reloaded] Failed to read config file.", e);
            return new ConfigOptions().defaultOptions();
        }
    }

    private static void applyConfig(ConfigOptions config) {
        if (config == null) {
            config = new ConfigOptions().defaultOptions();
        }

        CachedSkinProvider.setRememberSkin(config.rememberSkin);
        CachedCapeProvider.setRememberCape(config.rememberCape);

        SkinProviderAPI.SKIN.clearProviders();
        SkinProviderAPI.CAPE.clearProviders();

        SkinUtils.clearPlayersTextureSuppliers();
        Path cachedImages = Paths.get(".", "cachedImages");
        if (config.useCachedSkin) {
            SkinProviderAPI.SKIN.registerProvider(
                    new CachedSkinProvider(cachedImages).withFilter(ImageUtils::legacySkinFilter));
        }

        if (config.useMojang) {
            SkinProviderAPI.SKIN.registerProvider(new MojangSkinProvider().withFilter(ImageUtils::legacySkinFilter));
        }

        if (config.useCrafatar) {
            SkinProviderAPI.SKIN.registerProvider(new CrafatarSkinProvider().withFilter(ImageUtils::legacySkinFilter));
        }

        if (config.useCustomServer) {
            SkinProviderAPI.SKIN.registerProvider(
                    new CustomServerSkinProvider()
                            .setHost(config.linkCustomServerSkin)
                            .setAllowHd(config.allowHDPlayers)
                            .setMaxHDResolution(config.maxHDResolution)
                            .withFilter(ImageUtils::legacySkinFilter)
            );
        }
        if (config.useCachedCape) {
            SkinProviderAPI.CAPE.registerProvider(
                    new CachedCapeProvider(cachedImages).withFilter(ImageUtils::legacyCapeFilter));
        }

        if (config.useMojang) {
            SkinProviderAPI.CAPE.registerProvider(new MojangCapeProvider().withFilter(ImageUtils::legacyCapeFilter));
        }

        if (config.useCrafatar) {
            SkinProviderAPI.CAPE.registerProvider(new CrafatarCapeProvider().withFilter(ImageUtils::legacyCapeFilter));
        }

        if (config.useCustomServer) {
            SkinProviderAPI.CAPE.registerProvider(
                    new CustomServerCapeProvider()
                            .setHost(config.linkCustomServerCape)
                            .setAllowHd(config.allowHDPlayers)
                            .setMaxHDResolution(config.maxHDResolution)
                            .withFilter(ImageUtils::legacyCapeFilter)
            );
        }

        PLAYERHEADS = !config.disablePlayerHeads;
        lastLoadedConfig = config;
    }


    private final Map<UUID, RetryState> skinInitialization = new HashMap<>();
    private static long lastInternetCheck;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_DELAY_TICKS = 20;

    public static void recacheSkins() {
        Minecraft client = Minecraft.getInstance();

        if (client.level != null) {
            boolean smooth = lastLoadedConfig.smoothInitialization;

            if (!smooth) {
                SkinUtils.clearPlayersTextureSuppliers();

                for (Player player : client.level.players()) {
                    GameProfile gameProfile = player.getGameProfile();
                    PlayerProfile profile = PlayerProfile.wrapGameProfile(gameProfile);

                    delaySkinAppearance(gameProfile);
                    SkinProviderAPI.SKIN.recache(profile);
                }

                return;
            }

            for (Player player : client.level.players()) {
                GameProfile gameProfile = player.getGameProfile();
                PlayerProfile profile = PlayerProfile.wrapGameProfile(gameProfile);

                long delay = smoothInitialization();

                runSmoothInitialization(delay, () -> {
                    SkinUtils.clearPlayerTextureSuppliers(profile.getPlayerName());
                    delaySkinAppearance(gameProfile);
                    SkinProviderAPI.SKIN.recache(profile);
                });
            }
        }
    }

    public static void recacheSkin() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            GameProfile gameProfile = client.player.getGameProfile();
            PlayerProfile profile = PlayerProfile.wrapGameProfile(gameProfile);

            SkinUtils.clearPlayerTextureSuppliers(profile.getPlayerName());

            delaySkinAppearance(gameProfile);

            CachedSkinProvider.refreshSelectedSkin(profile.getPlayerName());

            SkinProviderAPI.SKIN.recache(profile);
        }
    }

    public static void recacheCapes() {
        Minecraft client = Minecraft.getInstance();

        if (client.level != null) {
            boolean smooth = lastLoadedConfig.smoothInitialization;

            if (!smooth) {
                SkinUtils.clearPlayersTextureSuppliers();

                for (Player player : client.level.players()) {
                    GameProfile gameProfile = player.getGameProfile();
                    PlayerProfile profile = PlayerProfile.wrapGameProfile(gameProfile);

                    delayCapeAppearance(gameProfile);
                    SkinProviderAPI.CAPE.recache(profile);
                }

                return;
            }

            for (Player player : client.level.players()) {
                GameProfile gameProfile = player.getGameProfile();
                PlayerProfile profile = PlayerProfile.wrapGameProfile(gameProfile);

                long delay = smoothInitialization();

                runSmoothInitialization(delay, () -> {
                    SkinUtils.clearPlayerTextureSuppliers(profile.getPlayerName());
                    delayCapeAppearance(gameProfile);
                    SkinProviderAPI.CAPE.recache(profile);
                });
            }
        }
    }

    public static void recacheCape() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            GameProfile gameProfile = client.player.getGameProfile();
            PlayerProfile profile = PlayerProfile.wrapGameProfile(gameProfile);

            SkinUtils.clearPlayerTextureSuppliers(profile.getPlayerName());

            delayCapeAppearance(gameProfile);

            CachedCapeProvider.refreshSelectedCape(profile.getPlayerName());

            SkinProviderAPI.CAPE.recache(profile);
        }
    }

    @Override
    public void onInitializeClient() {
        KeyBindsAndCommands.ModKeyBindings.register();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level == null) {
                return;
            }

            long gameTime = mc.level.getGameTime();

            if (gameTime - lastInternetCheck >= 1200) {
                if (lastLoadedConfig.smartInternetCheck)
                    SmartInternetCheck.check();

                lastInternetCheck = gameTime;
            }

            for (Player player : mc.level.players()) {
                UUID uuid = player.getUUID();

                RetryState state = skinInitialization.computeIfAbsent(uuid, k -> new RetryState(0, gameTime));

                if (state.attempts < MAX_ATTEMPTS && gameTime >= state.nextAttemptTick) {
                    PlayerProfile profile =
                            PlayerProfile.wrapGameProfile(player.getGameProfile());

                    SkinProviderAPI.SKIN.getSkin(profile);
                    SkinProviderAPI.CAPE.getSkin(profile);

                    state.attempts++;
                    state.nextAttemptTick = gameTime + RETRY_DELAY_TICKS;
                }
            }

            skinInitialization.keySet().removeIf(uuid ->
                    mc.level.players().stream().noneMatch(player -> player.getUUID().equals(uuid))
            );
        });

        reloadRuntime();
    }

    private static class RetryState {
        int attempts;
        long nextAttemptTick;

        RetryState(int attempts, long nextAttemptTick) {
            this.attempts = attempts;
            this.nextAttemptTick = nextAttemptTick;
        }
    }
}