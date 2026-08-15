package vorez.mods.skins.impl.fabric;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.resources.PlayerSkin;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class SkinUtils {

    private static final Function<GameProfile, ResourceLocation> SKIN = profile -> FabricOfflineSkinsReloaded.getLocationSkin(profile, null);
    private static final Function<GameProfile, ResourceLocation> CAPE = profile -> FabricOfflineSkinsReloaded.getLocationCape(profile, null);

    private static final Function<GameProfile, PlayerSkin.Model> MODEL = profile -> {
        String type = FabricOfflineSkinsReloaded.getSkinType(profile, null);
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
            .build(new CacheLoader<GameProfile, Supplier<PlayerSkin>>() {
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

    public static void clearTextureSuppliers() {
        textureSuppliers.invalidateAll();
        textureSuppliers.cleanUp();
    }
}
