package vorez.mods.skins.impl.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class SkinUtils {

    private static final Function<GameProfile, ResourceLocation> SKIN = FabricOfflineSkinsReloaded::getLocationSkin;
    private static final Function<GameProfile, ResourceLocation> CAPE = FabricOfflineSkinsReloaded::getLocationCape;

    private static final Function<GameProfile, PlayerSkin.Model> MODEL = profile -> {
        String type = FabricOfflineSkinsReloaded.getSkinType(profile);
        if (type == null) return PlayerSkin.Model.WIDE;
        try {
            return PlayerSkin.Model.valueOf(type.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PlayerSkin.Model.WIDE;
        }
    };

    private static final LoadingCache<GameProfile, Supplier<PlayerSkin>> textureSuppliers = CacheBuilder
            .newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Supplier<PlayerSkin> load(GameProfile profile) {
                    AtomicReference<PlayerSkin> holder = new AtomicReference<>();
                    return () -> {
                        PlayerSkin textures = holder.get();
                        ResourceLocation skinTexture = SKIN.apply(profile);
                        ResourceLocation capeTexture = CAPE.apply(profile);

                        PlayerSkin.Model model = MODEL.apply(profile);

                        if (textures == null) {
                            if (skinTexture != null) {
                                PlayerSkin created = new PlayerSkin(
                                        skinTexture,
                                        null,
                                        capeTexture,
                                        null,
                                        model,
                                        true
                                );
                                if (!holder.compareAndSet(null, created)) {
                                    textures = holder.get();
                                } else {
                                    textures = created;
                                }
                            }
                        } else if (skinTexture != null) {
                            ResourceLocation currentSkin = textures.texture();
                            ResourceLocation currentCape = textures.capeTexture();

                            if (!skinTexture.equals(currentSkin) || !Objects.equals(capeTexture, currentCape) || textures.model() != model) {
                                PlayerSkin created = new PlayerSkin(
                                        skinTexture,
                                        null,
                                        capeTexture,
                                        null,
                                        model,
                                        true
                                );
                                if (!holder.compareAndSet(textures, created)) {
                                    textures = holder.get();
                                } else {
                                    textures = created;
                                }
                            }
                        }

                        return textures;
                    };
                }
            });

    public static PlayerSkin textures(GameProfile profile) {
        return textureSuppliers.getUnchecked(profile).get();
    }

    public static void clearPlayersTextureSuppliers() {
        textureSuppliers.invalidateAll();
        textureSuppliers.cleanUp();
    }

    public static void clearPlayerTextureSuppliers(String playerName) {
        for(GameProfile profile : textureSuppliers.asMap().keySet()) {
            if (playerName.equalsIgnoreCase(profile.getName())) {
                textureSuppliers.invalidate(profile);
            }
        }
    }
}
