package vorez.mods.skins.impl.fabric;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class SkinUtils {

    private static final Function<GameProfile, ResourceLocation> SKIN = profile -> FabricOfflineSkinsReloaded.getLocationSkin(profile, null);
    private static final Function<GameProfile, ResourceLocation> CAPE = profile -> FabricOfflineSkinsReloaded.getLocationCape(profile, null);

    private static final Function<GameProfile, PlayerModelType> MODEL = profile -> {
        String type = FabricOfflineSkinsReloaded.getSkinType(profile, null);
        if (type == null) return PlayerModelType.WIDE;
        try {
            return PlayerModelType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PlayerModelType.WIDE;
        }
    };

    private static ClientAsset.ResourceTexture textureAsset(ResourceLocation id) {
        return id == null ? null : new ClientAsset.ResourceTexture(id, id);
    }

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
                        PlayerModelType model = MODEL.apply(profile);

                        if (textures == null) {
                            if (skinTexture != null) {
                                PlayerSkin created = new PlayerSkin(
                                        textureAsset(skinTexture),
                                        textureAsset(capeTexture),
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
                            ResourceLocation currentSkin = textures.body() != null ? textures.body().id() : null;
                            ResourceLocation currentCape = textures.cape() != null ? textures.cape().id() : null;

                            if (!skinTexture.equals(currentSkin) || !Objects.equals(capeTexture, currentCape) || textures.model() != model) {
                                PlayerSkin created = new PlayerSkin(
                                        textureAsset(skinTexture),
                                        textureAsset(capeTexture),
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
