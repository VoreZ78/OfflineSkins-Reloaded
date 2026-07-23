package lain.mods.skins.init.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import lain.mods.skins.api.SkinProviderAPI;
import lain.mods.skins.api.interfaces.ISkin;
import lain.mods.skins.impl.ConfigOptions;
import lain.mods.skins.impl.PlayerProfile;
import lain.mods.skins.impl.fabric.ImageUtils;
import lain.mods.skins.providers.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FabricOfflineSkins implements ClientModInitializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get(".", "config", "offlineskins.json");
    private static final Map<String, Identifier> textures = new ConcurrentHashMap<>();

    public static boolean PLAYERHEADS = true;

    private static volatile ConfigOptions lastLoadedConfig = new ConfigOptions().defaultOptions();

    private static Identifier generateRandomLocation() {
        return Identifier.of("offlineskins", String.format("textures/generated/%s", UUID.randomUUID()));
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

    public static Identifier getLocationCape(GameProfile profile, Identifier result) {
        ISkin skin = SkinProviderAPI.CAPE.getSkin(PlayerProfile.wrapGameProfile(profile));
        if (skin != null && skin.isDataReady()) {
            ByteBuffer data = skin.getData();
            if (data != null) {
                return getOrCreateTextureNullable(data, skin);
            }
        }
        return null;
    }

    public static Identifier getLocationSkin(GameProfile profile, Identifier result) {
        ISkin skin = SkinProviderAPI.SKIN.getSkin(PlayerProfile.wrapGameProfile(profile));
        if (skin != null && skin.isDataReady()) {
            ByteBuffer data = skin.getData();
            if (data != null) {
                return getOrCreateTextureNullable(data, skin);
            }
        }
        return null;
    }

    private static Identifier registerTexture(ByteBuffer data, ISkin skin, String key) throws IOException {
        Identifier location = generateRandomLocation();
        ByteBuffer readBuffer = data.asReadOnlyBuffer();
        readBuffer.rewind();
        NativeImageBackedTexture texture = new NativeImageBackedTexture(location::toString, NativeImage.read(readBuffer));
        MinecraftClient client = MinecraftClient.getInstance();
        client.getTextureManager().registerTexture(location, texture);
        texture.upload();
        textures.put(key, location);

        if (skin != null) {
            skin.setRemovalListener(s -> {
                ByteBuffer removedData = s.getData();
                if (removedData != null && key.equals(textureKey(removedData))) {
                    client.execute(() -> {
                        client.getTextureManager().destroyTexture(location);
                        textures.remove(key, location);
                    });
                }
            });
        }
        return location;
    }

    private static Identifier getOrCreateTexture(ByteBuffer data, ISkin skin) throws IOException {
        String key = textureKey(data);
        if (key == null) {
            return null;
        }

        Identifier existing = textures.get(key);
        if (existing != null) {
            return existing;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            return registerTexture(data, skin, key);
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Identifier> result = new AtomicReference<>();
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

    private static Identifier getOrCreateTextureNullable(ByteBuffer data, ISkin skin) {
        try {
            return getOrCreateTexture(data, skin);
        } catch (IOException e) {
            return null;
        }
    }

    public static String getSkinType(GameProfile profile, String result) {
        Identifier location = getLocationSkin(profile, null);
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
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("[OfflineSkins] Failed to write config file.");
        }
    }

    public static synchronized void reloadRuntime() {
        ConfigOptions config = loadConfigSnapshot();
        applyConfig(config);
    }

    private static ConfigOptions loadConfigFromDisk() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!CONFIG_PATH.toFile().exists()) {
            saveConfigFile(new ConfigOptions().defaultOptions());
        }

        try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            ConfigOptions config = GSON.fromJson(json, ConfigOptions.class);
            if (config == null) {
                config = new ConfigOptions().defaultOptions();
            }
            config.validate();
            return config;
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("[OfflineSkins] Failed to read config file.");
            return new ConfigOptions().defaultOptions();
        }
    }

    private static void applyConfig(ConfigOptions config) {
        if (config == null) {
            config = new ConfigOptions().defaultOptions();
        }

        SkinProviderAPI.SKIN.clearProviders();
        SkinProviderAPI.SKIN.registerProvider(new UserManagedSkinProvider(Paths.get(".", "cachedImages")).withFilter(ImageUtils::legacyFilter));
        if (config.useCustomServer) {
            SkinProviderAPI.SKIN.registerProvider(new CustomServerSkinProvider().setHost(config.hostCustomServer).withFilter(ImageUtils::legacyFilter));
        }
        if (config.useCustomServer2) {
            SkinProviderAPI.SKIN.registerProvider(new CustomServerSkinProvider2().setHost(config.hostCustomServer2Skin).withFilter(ImageUtils::legacyFilter));
        }
        if (config.useMojang) {
            SkinProviderAPI.SKIN.registerProvider(new MojangSkinProvider().withFilter(ImageUtils::legacyFilter));
        }
        if (config.useCrafatar) {
            SkinProviderAPI.SKIN.registerProvider(new CrafatarSkinProvider().withFilter(ImageUtils::legacyFilter));
        }

        SkinProviderAPI.CAPE.clearProviders();
        SkinProviderAPI.CAPE.registerProvider(new UserManagedCapeProvider(Paths.get(".", "cachedImages")));
        if (config.useCustomServer) {
            SkinProviderAPI.CAPE.registerProvider(new CustomServerCapeProvider().setHost(config.hostCustomServer));
        }
        if (config.useCustomServer2) {
            SkinProviderAPI.CAPE.registerProvider(new CustomServerCapeProvider2().setHost(config.hostCustomServer2Cape));
        }
        if (config.useMojang) {
            SkinProviderAPI.CAPE.registerProvider(new MojangCapeProvider());
        }
        if (config.useCrafatar) {
            SkinProviderAPI.CAPE.registerProvider(new CrafatarCapeProvider());
        }

        PLAYERHEADS = !config.disablePlayerHeads;
        lastLoadedConfig = config;
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.world != null) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    SkinProviderAPI.SKIN.getSkin(PlayerProfile.wrapGameProfile(player.getGameProfile()));
                    SkinProviderAPI.CAPE.getSkin(PlayerProfile.wrapGameProfile(player.getGameProfile()));
                }
            }
        });

        reloadRuntime();
    }
}
